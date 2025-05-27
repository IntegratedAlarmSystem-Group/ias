Name:           ias
Version:        13.1.3
Release:        1%{?dist}
Summary:        Install the Integrated Alarm System
BuildArch:      noarch

License:        LGPL-3.0-only
URL:            https://github.com/IntegratedAlarmSystem-Group/ias
Source0:        %{url}/archive/refs/tags/%{version}.tar.gz#/%{name}-%{version}.tar.gz

#
# TODO Missing dependency on jep
#
BuildRequires:  (java-17-openjdk-devel or java-21-openjdk-devel or java-lastest-openjdk-devel)
BuildRequires:  javapackages-tools
BuildRequires:  python3-devel >= 3.10
BuildRequires:	%{py3_dist confluent-kafka}
BuildRequires:	%{py3_dist python-dateutil}
BuildRequires:	%{py3_dist pyyaml}
BuildRequires:	%{py3_dist sqlalchemy}
BuildRequires:	%{py3_dist oracledb}

Requires:  python3 >= 3.10
Requires:  (java-17-openjdk-headless or java-21-openjdk-headless or java-latest-openjdk-headless)
Requires:  %{py3_dist confluent-kafka}
Requires:  %{py3_dist python-dateutil}
Requires:  %{py3_dist pyyaml}
Requires:  %{py3_dist sqlalchemy}
Requires:  %{py3_dist oracledb}

# TODO Suggests: Kafka

%global syspython3_sitelib  /usr/lib/python%{python3_version}/site-packages
%global _prefix              /opt/IasRoot
%global sysbindir           /usr/bin

%description
The production version of the core of the Integrated Alarm System

%prep
%autosetup -n ias-%{version}

%build
export JAVA_HOME=%java_home
export IAS_ROOT=%{buildroot}%{_prefix}
./gradlew build 


%install
mkdir -p %{buildroot}%{_prefix}
export IAS_ROOT=%{buildroot}%{_prefix}
./gradlew install
# TODO permissions to be fixed in gradle build
chmod +x %{buildroot}%{_bindir}/*

# install pth file
echo %{python3_sitelib} > %{name}.pth
install -m644 -D -t %{buildroot}%{syspython3_sitelib} %{name}.pth

# make executables available via system path
mkdir -p %{buildroot}%{sysbindir}
for i in $(ls -1 %{buildroot}%{_bindir}/|grep -v Test)
do
    ln -v -s "%{_bindir}/$i" %{buildroot}%{sysbindir}/
done

# Create log and tmp foders
mkdir -p %{buildroot}%{_prefix}/logs
mkdir -p %{buildroot}%{_prefix}/tmp
chmod 777 %{buildroot}%{_prefix}/logs
chmod 777 %{buildroot}%{_prefix}/tmp

%check
# Run IAS uniit tests
export IAS_ROOT=%{buildroot}%{_prefix}
. ./Tools/config/ias-bash-profile.sh 
./gradlew iasUnitTest
./gradlew clean

%files
%dir %{_prefix}
 
%exclude %{_prefix}/LICENSE.md
%exclude %{_prefix}/README.md
%exclude %{_prefix}/RELEASE.txt
 
%dir %{_bindir}
%exclude %{_bindir}/MockUdpPlugin
%exclude %{_bindir}/*Test*
%{_bindir}/iasAlarmGui
%{_bindir}/iasBuildApiDocs
%{_bindir}/iasCalConverterTimes
%{_bindir}/iasCdbChecker
%{_bindir}/iasConverter
%{_bindir}/iasCreateModule
%{_bindir}/iasFindFile
%{_bindir}/iasGetClasspath
%{_bindir}/iasLTDBConnector
%{_bindir}/iasLogDumper
%{_bindir}/iasMailSender
%{_bindir}/iasMonitor
%{_bindir}/iasRun
%{_bindir}/iasRunningTools
%{_bindir}/iasSendCmd
%{_bindir}/iasSupervisor
%{_bindir}/iasWebServerSender
%{_bindir}/startIasServices
%{_bindir}/stopIasServices
 
%dir %{_prefix}/config
%{_prefix}/config/FoldersOfAModule.template
%exclude %{_prefix}/config/LPGPv3License.txt
%{_prefix}/config/LtdbCassandraConnector.properties
%{_prefix}/config/LtdbCassandraStandalone.properties
%{_prefix}/config/ias-bash-profile.sh
%{_prefix}/config/kafka-connect-log4j.xml
%exclude %{_prefix}/config/ias.spec
%{_prefix}/config/kafka_kraft_server.properties
%{_prefix}/config/logback.xml
 
%dir %{_prefix}/lib
%{_prefix}/lib/ExtLibs/
%{_prefix}/lib/ias*.jar
 
%dir %{python3_sitelib}
%{python3_sitelib}/IASApiDocs/
%{python3_sitelib}/IASLogging/
%{python3_sitelib}/IASTools/
%{python3_sitelib}/IasAlarmGui/
%{python3_sitelib}/IasBasicTypes/
%{python3_sitelib}/IasCdb/
%{python3_sitelib}/IasCmdReply/
%{python3_sitelib}/IasHeartbeat/
%{python3_sitelib}/IasKafkaUtils/
%{python3_sitelib}/IasPlugin2/
%{python3_sitelib}/IasPlugin3/
%{python3_sitelib}/IasTransferFunction/
%{python3_sitelib}/TestTF/
 
%dir %{_prefix}/logs
%dir %{_prefix}/tmp
 
%exclude %{sysbindir}/MockUdpPlugin
%{sysbindir}/iasAlarmGui
%{sysbindir}/iasBuildApiDocs
%{sysbindir}/iasCalConverterTimes
%{sysbindir}/iasCdbChecker
%{sysbindir}/iasConverter
%{sysbindir}/iasCreateModule
%{sysbindir}/iasFindFile
%{sysbindir}/iasGetClasspath
%{sysbindir}/iasLTDBConnector
%{sysbindir}/iasLogDumper
%{sysbindir}/iasMailSender
%{sysbindir}/iasMonitor
%{sysbindir}/iasRun
%{sysbindir}/iasRunningTools
%{sysbindir}/iasSendCmd
%{sysbindir}/iasSupervisor
%{sysbindir}/iasWebServerSender
%{sysbindir}/startIasServices
%{sysbindir}/stopIasServices
 
%license LICENSES/*
%{syspython3_sitelib}/%{name}.pth

%changelog
* Wed May 08 2024 acaproni Creation of the SPEC
- 
