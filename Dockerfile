FROM centos:7

# Install Java and Ant
RUN yum update -y && \
  yum install java-1.8.0-openjdk \
  java-1.8.0-openjdk-devel \
  ant \
  ant-contrib \
  wget \
  -y


# Install Kafka
RUN wget http://www-us.apache.org/dist/kafka/2.0.0/kafka_2.12-2.0.0.tgz && \
  tar -zxvf kafka_2.12-2.0.0.tgz && \
  mv kafka_2.12-2.0.0 /opt/

# Install Scala
RUN wget https://www.scala-lang.org/files/archive/scala-2.12.6.rpm && \
  rpm -i scala-2.12.6.rpm

# Install Python 3
RUN yum install https://centos7.iuscommunity.org/ius-release.rpm -y && \
  yum install python36u -y && \
  update-alternatives --install /usr/bin/python python /usr/bin/python3.6 1

# Build source code
RUN mkdir -p /usr/src/IntegratedAlarmSystemRoot
WORKDIR /usr/src/ias
COPY . /usr/src/ias

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk
ENV JRE_HOME $JAVA_HOME/jre
ENV KAFKA_HOME /opt/kafka_2.12-2.0.0
ENV SCALA_HOME /usr/share/scala
ENV IAS_ROOT /usr/src/IntegratedAlarmSystemRoot

RUN bash -c "source Tools/config/ias-bash-profile.sh && ant build"
ENV PATH $PATH:/usr/src/ias/Tools/bin
EXPOSE 2181 9092
