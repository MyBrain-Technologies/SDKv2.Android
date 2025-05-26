#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "MyBrainTechSDK::SNR" for configuration "Release"
set_property(TARGET MyBrainTechSDK::SNR APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(MyBrainTechSDK::SNR PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/x86_64/libSNR.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS MyBrainTechSDK::SNR )
list(APPEND _IMPORT_CHECK_FILES_FOR_MyBrainTechSDK::SNR "${_IMPORT_PREFIX}/lib/x86_64/libSNR.a" )


# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
