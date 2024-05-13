Name:           ias
Version:        13.1.1
Release:        1%{?dist}
Summary:        Install the Integrated Alarm System
BuildArch:      noarch

License:        LGPL-3.0-or-later 
URL:            https://integratedalarmsystem-group.github.io
Source0:        ias.tgz

BuildRequires:  bash  
BuildRequires:  tar  
BuildRequires:  zip unzip  
BuildRequires:  java-devel-openjdk >= 17.0.10.0.7

Requires:       python3 >= 3.10
Requires:	python3-confluent-kafka
Requires:	(java-headless >= 17.0.10.0.7 or jre-openjdk >= 17.0.10.0.7)

%description
This RPM installs the Integrated Alarm System (IAS)

%prep
%setup -n ias
mkdir -p %{_builddir}/IasRoot
rm -rf %{_builddir}/IasRoot/*

%build
cd %{_builddir}/ias
export IAS_ROOT=%{_builddir}/IasRoot
./gradlew build install

%check
# Run IAS uniit tests
cd %{_builddir}/ias
export IAS_ROOT=%{_builddir}/IasRoot 
. %{_builddir}/ias/Tools/config/ias-bash-profile.sh 
./gradlew iasUnitTest

%install
mkdir -p %{buildroot}/opt/IasRoot
cp -av %{_builddir}/IasRoot/* %{buildroot}/opt/IasRoot

%files
#%license add-license-file-here
#%doc add-docs-here



%changelog
* Wed May 08 2024 acaproni Creation of the SPEC
- 
