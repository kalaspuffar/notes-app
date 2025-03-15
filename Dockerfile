
# Build in a container with Oracle GraalVM Native Image and MUSL
FROM container-registry.oracle.com/graalvm/native-image:23-muslib AS nativebuild
WORKDIR /build
# Install UPX
ARG UPX_VERSION=4.2.2
ARG UPX_ARCHIVE=upx-${UPX_VERSION}-amd64_linux.tar.xz
RUN microdnf -y install wget xz && \
    wget -q https://github.com/upx/upx/releases/download/v${UPX_VERSION}/${UPX_ARCHIVE} && \
    tar -xJf ${UPX_ARCHIVE} && \
    rm -rf ${UPX_ARCHIVE} && \
    mv upx-${UPX_VERSION}-amd64_linux/upx . && \
    rm -rf upx-${UPX_VERSION}-amd64_linux

ARG MAVEN_VERSION=3.9.9
ARG MAVEN_ARCHIVE=apache-maven-${MAVEN_VERSION}-bin.tar.gz
RUN wget -q https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE} && \
    tar -xzf ${MAVEN_ARCHIVE} && \
    rm -rf ${MAVEN_ARCHIVE}

COPY pom.xml .
ADD src src

RUN apache-maven-${MAVEN_VERSION}/bin/mvn clean package

# Build a native executable with native-image
RUN native-image -Os --static --libc=musl -jar target/notes-app-1.0-SNAPSHOT-jar-with-dependencies.jar -o notes
RUN ls -lh notes

# Compress the executable with UPX
RUN ./upx --lzma --best -o notes.upx notes
RUN ls -lh notes.upx

# Copy the compressed executable into a scratch container
FROM scratch
COPY --from=nativebuild /build/notes.upx /notes.upx
COPY static /static
ENTRYPOINT ["/notes.upx"]