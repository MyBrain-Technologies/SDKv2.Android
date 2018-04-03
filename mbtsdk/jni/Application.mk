NDK_TOOLCHAIN_VERSION := clang
# APP_STL := stlport_shared  --> does not seem to contain C++11 features
#APP_STL := gnustl_shared
APP_STL := c++_shared

APP_OPTIM := release

APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
APP_PLATFORM = 21
