FROM openjdk:8

# Install Apache Ant
RUN apt-get update && apt-get -y install ant ant-contrib

# Install Scala
RUN wget http://scala-lang.org/files/archive/scala-2.12.1.deb && \
    dpkg -i scala-2.12.1.deb && \
    apt-get update && \
    apt-get -y install scala

# Install Python 3
RUN apt-get update && apt-get install python3 -y && \
    update-alternatives --install /usr/bin/python python /usr/bin/python3 1

RUN mkdir -p /usr/src/IntegratedAlarmSystemRoot
WORKDIR /usr/src/ias
COPY . /usr/src/ias

ENV JRE_HOME $JAVA_HOME/jre
ENV SCALA_HOME /usr/share/scala
ENV IAS_ROOT /usr/src/IntegratedAlarmSystemRoot

RUN bash -c "source Tools/config/ias-bash-profile.sh && ant build"

ENV PATH $PATH:/usr/src/ias/Tools/bin

COPY entrypoint.sh /usr/src/ias
ENTRYPOINT ["./entrypoint.sh"]
