FROM centos:7

# Install Apache Ant
RUN yum update -y && \
  yum install java-1.8.0-openjdk \
  ant \
  ant-contrib \
  wget \
  -y

# Install Scala
RUN wget https://www.scala-lang.org/files/archive/scala-2.12.4.rpm && \
  rpm -i scala-2.12.4.rpm

# Install Python 3
RUN yum install https://centos7.iuscommunity.org/ius-release.rpm -y && \
  yum install python36u -y && \
  update-alternatives --install /usr/bin/python python /usr/bin/python3.6 1

# Build source code
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
