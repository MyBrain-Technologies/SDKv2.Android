#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "SignalProcessing::DataManipulation" for configuration "Release"
set_property(TARGET SignalProcessing::DataManipulation APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(SignalProcessing::DataManipulation PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/armeabi-v7a/libDataManipulation.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS SignalProcessing::DataManipulation )
list(APPEND _IMPORT_CHECK_FILES_FOR_SignalProcessing::DataManipulation "${_IMPORT_PREFIX}/lib/armeabi-v7a/libDataManipulation.a" )

# Import target "SignalProcessing::Transformations" for configuration "Release"
set_property(TARGET SignalProcessing::Transformations APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(SignalProcessing::Transformations PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/armeabi-v7a/libTransformations.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS SignalProcessing::Transformations )
list(APPEND _IMPORT_CHECK_FILES_FOR_SignalProcessing::Transformations "${_IMPORT_PREFIX}/lib/armeabi-v7a/libTransformations.a" )

# Import target "SignalProcessing::Algebra" for configuration "Release"
set_property(TARGET SignalProcessing::Algebra APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(SignalProcessing::Algebra PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/armeabi-v7a/libAlgebra.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS SignalProcessing::Algebra )
list(APPEND _IMPORT_CHECK_FILES_FOR_SignalProcessing::Algebra "${_IMPORT_PREFIX}/lib/armeabi-v7a/libAlgebra.a" )

# Import target "SignalProcessing::PreProcessing" for configuration "Release"
set_property(TARGET SignalProcessing::PreProcessing APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(SignalProcessing::PreProcessing PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/armeabi-v7a/libPreProcessing.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS SignalProcessing::PreProcessing )
list(APPEND _IMPORT_CHECK_FILES_FOR_SignalProcessing::PreProcessing "${_IMPORT_PREFIX}/lib/armeabi-v7a/libPreProcessing.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
