#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "FFTW3::fftw3" for configuration "Release"
set_property(TARGET FFTW3::fftw3 APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(FFTW3::fftw3 PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "C"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/arm64-v8a/libfftw3.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS FFTW3::fftw3 )
list(APPEND _IMPORT_CHECK_FILES_FOR_FFTW3::fftw3 "${_IMPORT_PREFIX}/lib/arm64-v8a/libfftw3.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
