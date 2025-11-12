Name: kafka
Version: 4.1.0
Release: 1%{?dist}
Summary: Install Apache Kafka 
License: Apache License 2.0
URL: https://kafka.apache.org
Source0: kafka-%{version}-src.tgz
Source1: kafka.service
Source2: server.properties
Source3: log4j2.yaml
BuildRequires: (java-17-openjdk-devel or java-21-openjdk-devel or java-latest-openjdk-devel)
BuildArch: noarch

Requires: (java-17-openjdk-headless or java-21-openjdk-headless or java-latest-openjdk-headless)

%global sysbindir    /usr/bin
%global _kafka       kafka_2.13-%{version}

%description
The Apache Kafka distributed event streaming platform using Kraft.

%prep
%setup -q -n kafka-%{version}-src

%build
./gradlew clean releaseTarGz -PscalaVersion=2.13.15

%install
# Create necessary directories
mkdir -p %{buildroot}/opt
mkdir -p %{buildroot}/var/lib/kafka/data
mkdir -p %{buildroot}/var/log/kafka
mkdir -p %{buildroot}/etc/systemd/system

# Extract Kafka to the correct location
tar -xf core/build/distributions/%{_kafka}.tgz -C %{buildroot}/opt
ln -v -s "/opt/%{_kafka}" %{buildroot}/opt/kafka

# Install configuration and systemd service file
mkdir -p %{buildroot}/opt/kafka_2.13-%{version}/config/kraft
install -m 644 %{SOURCE2} %{buildroot}/opt/%{_kafka}/config/server.properties
install -m 744 %{SOURCE3} %{buildroot}/opt/%{_kafka}/config/log4j2.yaml
install -m 644 %{SOURCE1} %{buildroot}/etc/systemd/system/kafka.service

install -Dm644 LICENSE %{buildroot}/opt/%{_kafka}/LICENSE.txt
mkdir -p %{buildroot}/usr/share/licenses/kafka
cp -a %{buildroot}/opt/%{_kafka}/licenses/* %{buildroot}/usr/share/licenses/kafka/

# make executables available via system path
mkdir -p %{buildroot}%{sysbindir}
for i in $(ls -1 %{buildroot}/opt/%{_kafka}/bin/); do
    echo "#!/usr/bin/bash" > %{buildroot}%{sysbindir}/$i
    echo "exec /opt/%{_kafka}/bin/$i \"\$@\"" >> %{buildroot}%{sysbindir}/$i
    chmod 0755 %{buildroot}%{sysbindir}/$i
done


%files
%license /usr/share/licenses/kafka
/opt/%{_kafka}/LICENSE.txt

/opt/kafka

%exclude %{sysbindir}/windows
%exclude /opt/%{_kafka}/bin/windows

/opt/%{_kafka}/NOTICE

/opt/%{_kafka}/bin/connect-distributed.sh
/opt/%{_kafka}/bin/connect-mirror-maker.sh
/opt/%{_kafka}/bin/connect-plugin-path.sh
/opt/%{_kafka}/bin/connect-standalone.sh
/opt/%{_kafka}/bin/kafka-acls.sh
/opt/%{_kafka}/bin/kafka-broker-api-versions.sh
/opt/%{_kafka}/bin/kafka-client-metrics.sh
/opt/%{_kafka}/bin/kafka-cluster.sh
/opt/%{_kafka}/bin/kafka-configs.sh
/opt/%{_kafka}/bin/kafka-console-consumer.sh
/opt/%{_kafka}/bin/kafka-console-producer.sh
/opt/%{_kafka}/bin/kafka-console-share-consumer.sh
/opt/%{_kafka}/bin/kafka-consumer-groups.sh
/opt/%{_kafka}/bin/kafka-consumer-perf-test.sh
/opt/%{_kafka}/bin/kafka-delegation-tokens.sh
/opt/%{_kafka}/bin/kafka-delete-records.sh
/opt/%{_kafka}/bin/kafka-dump-log.sh
/opt/%{_kafka}/bin/kafka-e2e-latency.sh
/opt/%{_kafka}/bin/kafka-features.sh
/opt/%{_kafka}/bin/kafka-get-offsets.sh
/opt/%{_kafka}/bin/kafka-groups.sh
/opt/%{_kafka}/bin/kafka-jmx.sh
/opt/%{_kafka}/bin/kafka-leader-election.sh
/opt/%{_kafka}/bin/kafka-log-dirs.sh
/opt/%{_kafka}/bin/kafka-metadata-quorum.sh
/opt/%{_kafka}/bin/kafka-metadata-shell.sh
/opt/%{_kafka}/bin/kafka-producer-perf-test.sh
/opt/%{_kafka}/bin/kafka-reassign-partitions.sh
/opt/%{_kafka}/bin/kafka-replica-verification.sh
/opt/%{_kafka}/bin/kafka-run-class.sh
/opt/%{_kafka}/bin/kafka-server-start.sh
/opt/%{_kafka}/bin/kafka-server-stop.sh
/opt/%{_kafka}/bin/kafka-share-consumer-perf-test.sh
/opt/%{_kafka}/bin/kafka-share-groups.sh
/opt/%{_kafka}/bin/kafka-storage.sh
/opt/%{_kafka}/bin/kafka-streams-application-reset.sh
/opt/%{_kafka}/bin/kafka-streams-groups.sh
/opt/%{_kafka}/bin/kafka-topics.sh
/opt/%{_kafka}/bin/kafka-transactions.sh
/opt/%{_kafka}/bin/kafka-verifiable-consumer.sh
/opt/%{_kafka}/bin/kafka-verifiable-producer.sh
/opt/%{_kafka}/bin/kafka-verifiable-share-consumer.sh
/opt/%{_kafka}/bin/trogdor.sh

%{sysbindir}/connect-distributed.sh
%{sysbindir}/connect-mirror-maker.sh
%{sysbindir}/connect-plugin-path.sh
%{sysbindir}/connect-standalone.sh
%{sysbindir}/kafka-acls.sh
%{sysbindir}/kafka-broker-api-versions.sh
%{sysbindir}/kafka-client-metrics.sh
%{sysbindir}/kafka-cluster.sh
%{sysbindir}/kafka-configs.sh
%{sysbindir}/kafka-console-consumer.sh
%{sysbindir}/kafka-console-producer.sh
%{sysbindir}/kafka-console-share-consumer.sh
%{sysbindir}/kafka-consumer-groups.sh
%{sysbindir}/kafka-consumer-perf-test.sh
%{sysbindir}/kafka-delegation-tokens.sh
%{sysbindir}/kafka-delete-records.sh
%{sysbindir}/kafka-dump-log.sh
%{sysbindir}/kafka-e2e-latency.sh
%{sysbindir}/kafka-features.sh
%{sysbindir}/kafka-get-offsets.sh
%{sysbindir}/kafka-groups.sh
%{sysbindir}/kafka-jmx.sh
%{sysbindir}/kafka-leader-election.sh
%{sysbindir}/kafka-log-dirs.sh
%{sysbindir}/kafka-metadata-quorum.sh
%{sysbindir}/kafka-metadata-shell.sh
%{sysbindir}/kafka-producer-perf-test.sh
%{sysbindir}/kafka-reassign-partitions.sh
%{sysbindir}/kafka-replica-verification.sh
%{sysbindir}/kafka-run-class.sh
%{sysbindir}/kafka-server-start.sh
%{sysbindir}/kafka-server-stop.sh
%{sysbindir}/kafka-share-consumer-perf-test.sh
%{sysbindir}/kafka-share-groups.sh
%{sysbindir}/kafka-storage.sh
%{sysbindir}/kafka-streams-application-reset.sh
%{sysbindir}/kafka-streams-groups.sh
%{sysbindir}/kafka-topics.sh
%{sysbindir}/kafka-transactions.sh
%{sysbindir}/kafka-verifiable-consumer.sh
%{sysbindir}/kafka-verifiable-producer.sh
%{sysbindir}/kafka-verifiable-share-consumer.sh
%{sysbindir}/trogdor.sh

%license /opt/%{_kafka}/LICENSE

%attr(0755, kafka, kafka) /opt/%{_kafka}/config
%attr(0755, kafka, kafka) /opt/%{_kafka}/libs
%attr(0755, kafka, kafka) /opt/%{_kafka}/licenses
%attr(0755, kafka, kafka) /opt/%{_kafka}/site-docs


%attr(0755, kafka, kafka) /var/lib/kafka/data
%attr(0755, kafka, kafka) /var/lib/kafka
%attr(0755, kafka, kafka) /var/log/kafka
%attr(0644, root, root) /etc/systemd/system/kafka.service

%pre
# Create kafka user and group if they don’t exist
getent group kafka >/dev/null || groupadd --system kafka
getent passwd kafka >/dev/null || useradd --system -g kafka --no-create-home --shell /sbin/nologin kafka

%post
# Set proper ownership
chown -R kafka:kafka /opt/kafka_2.13-%{version}
chown -R kafka:kafka /var/lib/kafka/data
chown -R kafka:kafka /var/lib/kafka
chown -R kafka:kafka /var/log/kafka

# Reload systemd for the new service
systemctl daemon-reload

# Format the folder where kafka stores the logs
sudo -u kafka /opt/kafka/bin/kafka-storage.sh format -t$(uuidgen) -c /opt/kafka/config/server.properties

%preun
# Stop Kafka service before removal
if [ $1 -eq 0 ]; then
    systemctl stop kafka >/dev/null 2>&1 || true
    systemctl disable kafka >/dev/null 2>&1 || true
fi

%postun
# Remove kafka user/group if no other package is using them
if [ $1 -eq 0 ]; then
    getent passwd kafka >/dev/null && userdel kafka || true
    getent group kafka >/dev/null && groupdel kafka || true
fi
rm -rf /var/lib/kafka
rm -rf /var/log/kafka
rm -rf /opt/kafka*

%changelog
* Wed Nov 12 2025 Alessandro Caproni <acaproni@eso.org> - 4.1.0-1
- Updated for v4.1.0
* Fri May 23 2025 Alessandro Caproni <acaproni@eso.org> - 4.0.0-1
- Initial RPM release of Apache Kafka with KRaft

