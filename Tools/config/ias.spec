Name:           ias
Version:        13.1.1
Release:        1%{?dist}
Summary:        Install the Integrated Alarm System
BuildArch:      noarch

License:        LGPL-3.0-or-later 
URL:            https://github.com/IntegratedAlarmSystem-Group/ias
Source0:        %{url}/archive/refs/tags/%{version}.tar.gz#/%{name}-%{version}.tar.gz

BuildRequires:  (java-17-openjdk-devel or java-21-openjdk-devel or java-lastest-openjdk-devel)
#BuildRequires:  java-17-openjdk-devel
BuildRequires:  javapackages-tools
BuildRequires:  python3-devel >= 3.10
BuildRequires:	python3-confluent-kafka

Requires:   python3 >= 3.10
Requires:	python3-confluent-kafka
Requires:	(java-17-openjdk-headless or java-21-openjdk-headless or java-latest-openjdk-headless)

%global syspython3_sitelib  /usr/lib/python%{python3_version}/site-packages
%global _prefix             /opt/IasRoot

%description
The production version of the core of the Integrated Alarm System

%prep
%autosetup -n ias-%{version}

%build
export JAVA_HOME=%java_home
#cd %{_builddir}/ias
export IAS_ROOT=%{buildroot}%{_prefix}
./gradlew build 


%install
mkdir -p %{buildroot}%{_prefix}
export IAS_ROOT=%{buildroot}%{_prefix}
./gradlew install

# install pth file
echo %{python3_sitelib} > %{name}.pth
install -m644 -D -t %{buildroot}%{syspython3_sitelib} %{name}.pth

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
/opt/IasRoot
%{syspython3_sitelib}/%{name}.pth

%changelog
* Wed May 08 2024 acaproni Creation of the SPEC
- 
