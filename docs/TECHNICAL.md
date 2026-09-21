# AndroPods — technical notes

How the app reads AirPods battery over BLE, what was measured on real
hardware, and what is deliberately left out. The user-facing guide is the
[README](../README.md).

Built for **AirPods (2nd generation, 2019)** — A2031/A2032, case A1602/A1938 —
and developed on a Pixel 7.

## How it works

AirPods continuously broadcast an unencrypted BLE advertisement — Apple's "proximity pairing" beacon, manufacturer ID `0x004C`, message type `0x07` — carrying battery and lid state. The app scans for that beacon and decodes it.

The format is not documented by Apple. The layout used here comes from published academic reverse-engineering ([Celosia & Cunche, PETS 2020](https://petsymposium.org/popets/2020/popets-2020-0003.pdf)) cross-verified against four independent open-source implementations: [librepods](https://github.com/librepods-org/librepods), [LightPods](https://github.com/gi-os/LightPods), [airpods-on-android](https://github.com/ioscastaway/airpods-on-android) and [AirStatus](https://github.com/delphiki/AirStatus).

### Payload layout

Offsets from the first byte after the 2-byte company ID:

| Offset | Field |
|---|---|
| 0 | message type, `0x07` |
| 1 | payload length, `0x19` (25) |
| 3–4 | device model, big-endian — Gen 2 is `0x0F20` |
| 5 | status: primary-pod, in-case, in-ear bits |
| 6 | pods battery, two nibbles |
| 7 | high nibble charging flags, low nibble case battery |
| 8 | lid state and open counter |
| 11–26 | AES-encrypted tail (unused — Gen 2 populates the plaintext nibbles) |

Either pod can be the broadcaster. Status bit 5 says the left pod is primary; when it is clear, every left/right field is transposed. Battery nibbles are deciles, and `0xF` means *unknown* — a normal reading, not an error.

Two traps worth knowing, both covered by tests:

1. **Decode from `ScanRecord.getBytes()`, not `getManufacturerSpecificData()`.** The latter returns a `SparseArray` keyed by company ID that keeps only the last entry, so a Find My (`0x12`) frame in the same packet silently evicts the `0x07` frame.
2. **Filter on the company ID and type byte only, never the length byte** — it varies across firmware generations.

### Identity

The beacon cannot prove whose AirPods it came from: addresses rotate roughly every 15 minutes and every Gen 2 shares model `0x0F20`. So scanning runs *only while your bonded AirPods are connected over classic Bluetooth*, and among candidates the strongest signal wins with an 8 dB hysteresis margin so a passing stranger cannot hijack the readout. This also keeps the radio off whenever the pods are away.

## Not included

- **Ear detection / auto-pause.** The in-ear bits in the beacon are unreliable — they report "in ear" with the buds in a shut case. Real ear detection needs Apple's AAP protocol over a classic L2CAP socket, reachable only through a hidden API that can break on any Android release. The bits are parsed and exposed as `rawLeftInEar` / `rawRightInEar` for diagnostics, and drive nothing.
- **Battery over classic Bluetooth.** A dead end: `+IPHONEACCEV` over HFP is never delivered to apps, `getBatteryLevel()` returns -1, and `ACTION_BATTERY_LEVEL_CHANGED` carries no level.

## Permissions

`BLUETOOTH_SCAN` carries `neverForLocation`, which removes the `ACCESS_FINE_LOCATION` requirement and its prompt. Scan results are never used to derive location.

The foreground service is typed `connectedDevice`, required on Android 14+ for BLE work, and `BLUETOOTH_CONNECT` is held when it starts or the platform throws.

## Build

```bash
./gradlew assembleDebug        # APK
./gradlew testDebugUnitTest    # 57 unit tests
./gradlew lintDebug            # Android lint
```

Requires JDK 17+ and an Android SDK with API 36. On Windows, `./gradlew clean` can fail with a file lock while the daemon holds lint jars open; `./gradlew --stop` first.

## Verification status

Unit tests cover the parser and the tracker, including left/right transposition, the inverted lid bit, the `0xF` unknown sentinel, the Find My eviction trap, anti-flapping under RSSI jitter, rejection of the 15-byte connected frame and of non-Gen-2 models, and one frame captured from the real pair; plus the settings store, the theme keys and the battery colour bands (57 tests).

### On-device status (Pixel 7, Android 17 / API 37)

**Working end to end.** With the pods out of the case and connected, the app's own scan receives the full proximity-pairing frame at around -45 dBm every batch window and the ongoing notification reads `Left 100% — Right 90% — Case —` (case unknown while the pods are out of it, which is the `0xF` sentinel doing its job). The frame that got it there, now the first hardware-derived fixture in the tests:

```
1E FF 4C00 07 19 01 0F20 01 A9 8F 01 00 04 C0 D1 B9 25 94 25 F3 48 1F 56 89 E3 4E 40 39 EF
           ^^ type   ^^^^ Gen 2 ^^ status 0x01: right pod primary, nibbles flipped
           ^^ 0x19 = 25 bytes   ^^ pods 0xA9 -> left 100, right 90    ^^ case 0xF -> unknown
```

**The one setting that matters on this phone is batch delivery.** `ScanSettings.setReportDelay(5000)`. The controller's regular per-packet result path delivers *nothing* here — not one frame from pods held against the phone, in any scan mode, on any PHY, filtered or unfiltered — while its batch path delivers every window. An eight-way matrix (`MatrixScanProbeTest`) varying one setting at a time made this unambiguous:

| Configuration | Pods frames / 15 s |
|---|---|
| BALANCED, batch 5 s, legacy 1M, MaterialPods' filter | 3 |
| same, LOW_LATENCY | 3 |
| same, extended PHY ALL | 3 |
| same, type-07 filter | 3 |
| same, MATCH_NUM_MAX + AGGRESSIVE | 3 |
| **same, but regular delivery (delay 0)** | **0** |
| AndroPods' original config (regular, LOW_LATENCY, PHY ALL) | 0 |
| unfiltered, regular | 0 |

Three per window because the controller de-duplicates within a batch window; that is one fresh reading every 5 s, which is plenty. MaterialPods works on this phone because its bytecode uses `setReportDelay(4000)` (the platform rounds it to 5 s) — that, and nothing else in its recipe, is the difference. Whether the regular path is broken on every Pixel 7 / Android 17 or only this unit is unknown; batch delivery costs nothing and is kept regardless.

**Gen 2 does send the battery frame while connected.** An earlier draft here said the 25-byte frame never appeared in any state. It was measured on the broken path. The 15-byte `07 0F` variant with the MAC address in bytes 5-10 is real too, and the parser rejects it by length (`MIN_LENGTH = 20`: that frame carries 17 payload bytes, the battery frame 24) rather than misreading its address bytes as battery levels, which `MIN_LENGTH = 9` used to do.

**Earlier findings in this section, now understood.** The `targetSdk` 35-vs-36 comparison, the "only LOW_LATENCY delivers results" note, the "MATCH_NUM_MAX + AGGRESSIVE + PHY_LE_ALL_SUPPORTED are required" note and the Activity-vs-service discrepancy were all measured on the regular path while it was delivering essentially nothing, and their numbers were noise from whatever else the controller was doing at the time. None of them is a constraint. `targetSdk` is still 35 only because nobody has retested 36 on the batch path; do that before assuming either way.

**A paired-but-absent LE HID device also starves scanning.** Before any of the above was visible, a bonded "1810 selfie pro" (a BLE selfie-stick shutter, HID-over-GATT) that was switched off left the controller permanently armed for an LE auto-connect (`le_connectability_state: ARMED`, the stick alone in the filter accept list, HID state `BTA_HH_W4_CONN_ST`), re-armed at every Bluetooth enable. While armed, even Google's own Nearby scanners logged zero results for hours. Forgetting the device cleared it. If BLE goes quiet on a phone, check `dumpsys bluetooth_manager` for an accept-list entry that is never in range.

**Two bugs found on the way, both fixed:** `BondReceiver` was registered `RECEIVER_NOT_EXPORTED`, which on Android 14+ blocks broadcasts from the Bluetooth process (uid 1002 is not the system uid) — four ACL transitions produced zero callbacks, so the service never stopped or restarted on connect/disconnect; and the scan filter mask was `0x01`, which compares one *bit*, not one byte.

**Diagnostics kept in `androidTest`:** `RawScanProbeTest` (unfiltered regular scan), `BatchScanProbeTest`, `MatrixScanProbeTest` (the table above), `AdvertiseProbeTest` (makes the phone a known transmitter; needs the debug-only `BLUETOOTH_ADVERTISE`). Install both APKs with `adb install -r -g` and run them with `am instrument -e seconds N`, not `connectedAndroidTest`, which reinstalls the app and wipes runtime grants — a probe launched that way silently scans without `BLUETOOTH_SCAN` and reports zero.

Remaining checks:

- [ ] Compare against MaterialPods or an iPhone showing the same pods. Current reading is left 100 / right 90 with `n*10`; this is the check that confirms `n*10` over the `n*10+5` variant and that left and right are not transposed.
- [ ] Pods into the case, lid shut, lid open: case level should appear, and the lid-open popup should fire on the open edge only. Note that with the pods *out* of the case the lid byte reads `0x01`, which the parser calls "open", so the popup fires once when the pods are first seen in the ears; decide whether that is acceptable or gate it on `caseBattery != null`.
- [ ] Take one pod out, put it back, swap which pod is primary — readings must follow the physical bud.
- [ ] Screen off 30+ minutes — the notification must keep updating.
- [ ] `adb shell dumpsys deviceidle force-idle` — the service must survive Doze.
- [ ] Walk out of range and back; confirm clean disconnect and recovery now that `BondReceiver` actually receives ACL events.
- [ ] Somewhere with other AirPods nearby, confirm hysteresis does not latch onto a stranger's pair.
- [ ] Deny each permission at runtime; confirm the app degrades rather than crashes.
- [ ] Retest `targetSdk = 36` on the batch path.
- [ ] Battery drain over a normal day via Battery Historian.

## Compatibility

Targets Gen 2 specifically, and will not generalise without a second parse path. Newer AirPods moved battery into the encrypted tail (7-bit value, charging in bit 7), and AirPods 4 on firmware 8B39 emit no `0x07` frames at all in reproducible states. Gen 2's older firmware is the best-supported case.
