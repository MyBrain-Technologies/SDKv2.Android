FROM runmymind/docker-android-sdk 

MAINTAINER Denis LAMARD denis.lamard@mybraintech.com

#update all repos
RUN apt-get update

#upgrade debian
RUN apt-get upgrade -y

#change current folder
WORKDIR /opt

#update SDK
RUN /opt/android-sdk-linux/tools/bin/sdkmanager ndk-bundle

#install gradle
RUN wget https://services.gradle.org/distributions/gradle-4.10-bin.zip
RUN unzip -d /opt/gradle gradle-4.10-bin.zip
RUN export PATH=$PATH:/opt/gradle/gradle-4.10/bin
RUN rm gradle-4.10-bin.zip



