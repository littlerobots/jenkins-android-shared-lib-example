# Image adopted from https://github.com/alvr/alpine-android/blob/master/android-base/Dockerfile, but you can use any Dockerfile
# Note that SDK_PACKAGES are passed from the library to get the required components using the sdk manager
FROM adoptopenjdk/openjdk8:alpine
LABEL maintainer="Álvaro S. <alvaro@alvr.me>"

ENV SDK_TOOLS "4333796"
ENV ANDROID_HOME "/opt/sdk"
ENV PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools

# Install required dependencies
RUN apk add --no-cache bash git unzip wget && \
    apk add --virtual .rundeps $runDeps && \
    rm -rf /tmp/* && \
    rm -rf /var/cache/apk/*

# Download and extract Android Tools
RUN wget -q https://dl.google.com/android/repository/sdk-tools-linux-${SDK_TOOLS}.zip -O /tmp/tools.zip && \
    mkdir -p ${ANDROID_HOME} && \
    unzip -qq /tmp/tools.zip -d ${ANDROID_HOME} && \
    rm -v /tmp/tools.zip

# Install SDK Packages
RUN mkdir -p ~/.android/ && touch ~/.android/repositories.cfg && \
    yes | ${ANDROID_HOME}/tools/bin/sdkmanager "--licenses" && \
    ${ANDROID_HOME}/tools/bin/sdkmanager "platform-tools" && \    
    ${ANDROID_HOME}/tools/bin/sdkmanager "--update"

WORKDIR /home/android

# Platforms we need (defaults)
ARG SDK_PACKAGES="build-tools;28.0.3 platforms;android-28"
RUN sdkmanager $SDK_PACKAGES

# User for our build, depends on your system
RUN adduser -u 1000 -h /home/android -D jenkins
USER jenkins

# Common Gradle settings, customise as you see fit
ENV GRADLE_OPTS "-Xmx1600m -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.caching=true"
# Move the Gradle home / cache into the workspace dir
ENV GRADLE_USER_HOME ./gradle-cache
