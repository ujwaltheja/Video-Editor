# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

project("videosnap")

add_library(videosnap SHARED mlt_wrapper.cpp)

# If the project includes a local 'libs/include/Mlt.h' then configure MLT. This
# allows building without MLT present (useful for CI or contributors who don't
# have the native SDK).
if(EXISTS "${CMAKE_SOURCE_DIR}/libs/include/Mlt.h")
    message(STATUS "Found MLT headers; skipping MLT linking for now")
    include_directories(libs/include)
    # Note: To enable MLT linking, add the compiled MLT library files to jniLibs/<ABI>
    # and uncomment the line below
    # target_link_libraries(videosnap mlt)
else()
    message(WARNING "MLT headers not found at src/main/cpp/libs/include/Mlt.h; skipping link to mlt.\nTo enable, add MLT headers to src/main/cpp/libs/include and the library to jniLibs/<ABI>.")
endif()
