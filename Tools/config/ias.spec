Name:           ias
Version:        13.1.3
Release:        1%{?dist}
Summary:        Install the Integrated Alarm System
BuildArch:      noarch

License:        LGPL-3.0-only
URL:            https://github.com/IntegratedAlarmSystem-Group/ias
Source0:        %{url}/archive/refs/tags/%{version}.tar.gz#/%{name}-%{version}.tar.gz

BuildRequires:  (java-17-openjdk-devel or java-21-openjdk-devel or java-lastest-openjdk-devel)
BuildRequires:  javapackages-tools
BuildRequires:  python3-devel >= 3.10
BuildRequires:	%{py3_dist confluent-kafka}
BuildRequires:	python3-dateutil
BuildRequires:	%{py3_dist pyyaml}
BuildRequires:	%{py3_dist sqlalchemy}
BuildRequires:	%{py3_dist oracledb}

Requires:  python3 >= 3.10
Requires:  python3-confluent-kafka
Requires:  (java-17-openjdk-headless or java-21-openjdk-headless or java-latest-openjdk-headless)
Requires:  %{py3_dist confluent-kafka}
Requires:  %{py3_dist dateutil}
Requires:  %{py3_dist pyyaml}
Requires:  %{py3_dist sqlalchemy}
Requires:  %{py3_dist oracledb}

%global syspython3_sitelib  /usr/lib/python%{python3_version}/site-packages
%global prefix              /opt/IasRoot
%global sysbindir           /usr/bin

%description
The production version of the core of the Integrated Alarm System

%prep
%autosetup -n ias-%{version}

%build
export JAVA_HOME=%java_home
#cd %{_builddir}/ias
export IAS_ROOT=%{buildroot}%{prefix}
./gradlew build 


%install
mkdir -p %{buildroot}%{prefix}
export IAS_ROOT=%{buildroot}%{prefix}
./gradlew install

# make executables available via system path
mkdir -p %{buildroot}%{sysbindir}
for i in $(ls -1 %{buildroot}%{prefix}/bin/|grep -v Test)
do
    ln -v -s "%{buildroot}%{prefix}/bin/$i" %{buildroot}%{sysbindir}/
done

# install pth file
echo %{python3_sitelib} > %{name}.pth
install -m644 -D -t %{buildroot}%{syspython3_sitelib} %{name}.pth

# Create log and tmp foders
mkdir -p %{buildroot}%{prefix}/logs
mkdir -p %{buildroot}%{prefix}/tmp
chmod 777 %{buildroot}%{prefix}/logs
chmod 777 %{buildroot}%{prefix}/tmp

#%check
# Run IAS uniit tests
export IAS_ROOT=%{buildroot}%{prefix}
. ./Tools/config/ias-bash-profile.sh 
./gradlew iasUnitTest
./gradlew clean

%files
/opt/IasRoot
%license LICENSES/*
%{syspython3_sitelib}/%{name}.pth
%exclude /opt/IasRoot/config/ias.spec
%exclude /opt/IasRoot/bin/*Test

%changelog
* Wed May 08 2024 acaproni Creation of the SPEC
- 
