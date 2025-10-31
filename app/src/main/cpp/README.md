This folder contains native code used by the app (C++ + CMake).

Problem you may hit when building on a machine that doesn't have the MLT SDK available:

- CMake will fail with `fatal error: 'Mlt.h' file not found` if the MLT headers are missing.

How this project is configured to avoid that failure:

- The Android Gradle module `app` checks for `src/main/cpp/libs/include/Mlt.h` and will skip configuring the native `externalNativeBuild` when that header is missing. This allows the app to build without the native MLT integration.

To enable MLT native build (two options):

Option A — Add prebuilt MLT headers & libraries into the repo (simpler for local builds):

1. Create the following folders in the repo:
   - `app/src/main/cpp/libs/include/` and place the MLT headers (e.g. `Mlt.h`) there.
   - `app/src/main/jniLibs/<ABI>/` (for example `arm64-v8a` and `armeabi-v7a`) and place the prebuilt `libmlt.so` (and any other required .so files) there.
2. Remove the header-guard in `app/build.gradle` (or simply re-run the build — Gradle will detect the header and run the native build automatically).

Option B — Install MLT for Android and point CMake to system paths (more advanced):

1. Install or cross-compile MLT for Android for the ABIs you target.
2. Edit `app/src/main/cpp/CMakeLists.txt` to add the correct `include_directories(...)` and `link_directories(...)` pointing to the MLT headers and libraries on your machine or CI environment.
3. Ensure the ABI-specific libs are discoverable by the Android linker or copied to `app/src/main/jniLibs/<ABI>/`.

If you want, I can:
- Add a CMakeLists change that checks for the presence of `Mlt.h` and only adds the `mlt` target_link if present (so the native build won't fail even if `externalNativeBuild` is run), or
- Add a small Gradle task to print whether MLT is available and list expected include/lib paths.

If you'd like me to enable automatic MLT detection in `CMakeLists.txt`, tell me and I'll add a safe check that prevents the compilation error when headers are missing.

