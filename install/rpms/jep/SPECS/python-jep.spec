%global srcname jep

%global desc \
Java Embedded Python\
JEP embeds CPython in Java through JNI and is safe to use in a\
heavily threaded environment.\
\
Some benefits of embedding CPython in a JVM:\
    Using the native Python interpreter may be much faster than\
    alternatives.\
    Python is mature, well supported, and well documented.\
    Access to high quality Python modules, both native CPython\
    extensions and Python-based.\
    Compilers and assorted Python tools are as mature as the\
    language.\
    Python is an interpreted language, enabling scripting of\
    established Java code without requiring recompilation.\
    Both Java and Python are cross platform, enabling deployment\
    to different operating system.

Name:           python-%{srcname}
Version:        4.2.2 
Release:        1%{?dist}
Summary:        Embed Python in Java

License:        zlib
URL:            https://github.com/ninia/%{srcname}
Source0:        %{url}/archive/v%{version}.tar.gz#/%{srcname}-%{version}.tar.gz

%dnl FIXME It seems %{java_arches} is empty when evaluated by OBS and
%dnl consequently the package is not built.
%dnl ExclusiveArch:  %{java_arches}
ExclusiveArch:  %{java_arches} x86_64

BuildRequires:  python3-devel
BuildRequires:  python3dist(setuptools)
BuildRequires:  python3dist(wheel)
BuildRequires:  python3dist(pip)
BuildRequires:  python3dist(numpy)

BuildRequires:  java-devel
BuildRequires:  gcc


%description %desc

%package     -n python3-%{srcname}
Requires:       java-headless
Requires:       python3dist(numpy)
Summary:        Embed Python in Java
%{?python_provide:%python_provide python3-%{srcname}}

%description -n python3-%{srcname} %desc

%package javadoc
Summary:        Javadoc files for %{name}
BuildArch:      noarch

%description javadoc
%{summary}.


%prep
%autosetup -p1 -n%{srcname}-%{version}
find . -name \*.jar -print -delete

%generate_buildrequires
export JAVA_HOME=%{_prefix}/lib/jvm/java
%pyproject_buildrequires


%build
export JAVA_HOME=%{_prefix}/lib/jvm/java
%pyproject_wheel
%{__python3} setup.py javadoc


%install
export JAVA_HOME=%{_prefix}/lib/jvm/java
%pyproject_install

%pyproject_save_files %{srcname}

# install javadoc
install -dm755 %{buildroot}%{_javadocdir}/%{name}
cp -pr javadoc/* %{buildroot}%{_javadocdir}/%{name}


%check
export JAVA_HOME=%{_prefix}/lib/jvm/java
# be more verbose about tests, FIXME ugly hack!
sed -i -r 's:TextTestRunner\(:\0verbosity=2:' src/test/python/runtests.py
%{py3_test_envvars} java -cp build/java/\* jep.Run src/test/python/runtests.py


%files -n python3-%{srcname} -f %{pyproject_files} 
%license LICENSE
%doc README.rst
%doc AUTHORS release_notes/
%{_bindir}/%{srcname}

%files javadoc
%license LICENSE
%{_javadocdir}/%{name}/


%changelog
* Sat Apr 12 2025 Nicolas Benes <nbenes@eso.org> - 4.2.2-1
- new version: 3.9.1 -> 4.2.2
- remove patch for numpy.bool

* Mon Jul 24 2023 Raphael Groner <raphgro@fedoraproject.org> - 3.9.1-11 
- avoid deprecated direct call to setup.py as Python packaging guidelines
- add patch for deprecated numpy.bool

* Fri Jul 21 2023 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.1-10
- Rebuilt for https://fedoraproject.org/wiki/Fedora_39_Mass_Rebuild

* Tue Jun 13 2023 Python Maint <python-maint@redhat.com> - 3.9.1-9
- Rebuilt for Python 3.12

* Fri Jan 20 2023 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.1-8
- Rebuilt for https://fedoraproject.org/wiki/Fedora_38_Mass_Rebuild

* Fri Jul 22 2022 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.1-7
- Rebuilt for https://fedoraproject.org/wiki/Fedora_37_Mass_Rebuild

* Mon Jun 13 2022 Python Maint <python-maint@redhat.com> - 3.9.1-6
- Rebuilt for Python 3.11

* Sat Feb 05 2022 Jiri Vanek <jvanek@redhat.com> - 3.9.1-5
- Rebuilt for java-17-openjdk as system jdk

* Fri Jan 21 2022 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.1-4
- Rebuilt for https://fedoraproject.org/wiki/Fedora_36_Mass_Rebuild

* Fri Jul 23 2021 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.1-3
- Rebuilt for https://fedoraproject.org/wiki/Fedora_35_Mass_Rebuild

* Fri Jun 04 2021 Python Maint <python-maint@redhat.com> - 3.9.1-2
- Rebuilt for Python 3.10

* Mon Feb 01 2021 Raphael Groner <raphgro@fedoraproject.org> - 3.9.1-1
- bump to v3.9.1 to fix FTBFS and improve support for python 3.9+, rhbz#1792062 
- drop upstream patch

* Wed Jan 27 2021 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.0-8
- Rebuilt for https://fedoraproject.org/wiki/Fedora_34_Mass_Rebuild

* Wed Jul 29 2020 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.0-7
- Rebuilt for https://fedoraproject.org/wiki/Fedora_33_Mass_Rebuild

* Sat Jul 11 2020 Jiri Vanek <jvanek@redhat.com> - 3.9.0-6
- Rebuilt for JDK-11, see https://fedoraproject.org/wiki/Changes/Java11

* Sat Jun 06 2020 Raphael Groner <raphgro@fedoraproject.org> - 3.9.0-5
- add patch for python 3.9, avoid daemon thread, rhbz#1792062

* Tue May 26 2020 Miro Hrončok <mhroncok@redhat.com> - 3.9.0-4
- Rebuilt for Python 3.9

* Thu Jan 30 2020 Fedora Release Engineering <releng@fedoraproject.org> - 3.9.0-3
- Rebuilt for https://fedoraproject.org/wiki/Fedora_32_Mass_Rebuild

* Thu Oct 03 2019 Miro Hrončok <mhroncok@redhat.com> - 3.9.0-2
- Rebuilt for Python 3.8.0rc1 (#1748018)

* Sat Aug 31 2019 Raphael Groner <projects.rg@smart.ms> - 3.9.0-1
- new version

* Mon Aug 19 2019 Miro Hrončok <mhroncok@redhat.com> - 3.8.2-5
- Rebuilt for Python 3.8

* Fri Jul 26 2019 Fedora Release Engineering <releng@fedoraproject.org> - 3.8.2-4
- Rebuilt for https://fedoraproject.org/wiki/Fedora_31_Mass_Rebuild

* Fri May 10 2019 Raphael Groner <projects.rg@smart.ms> - 3.8.2-3
- adjust execution of tests to fix for python 3.8, rhbz#1706238
- fix path of module installation

* Sat Feb 02 2019 Fedora Release Engineering <releng@fedoraproject.org> - 3.8.2-2
- Rebuilt for https://fedoraproject.org/wiki/Fedora_30_Mass_Rebuild

* Mon Dec 17 2018 Raphael Groner <projects.rg@smart.ms> - 3.8.2-1
- new version

* Thu Jul 12 2018 Raphael Groner <projects.rg@smart.ms> - 3.8.0-1
- new version

* Sat Jul 07 2018 Raphael Groner <projects.rg@smart.ms> - 3.8-0.1.rc
- new version (RC) with support for Python 3.7
- generate javadoc because not provided in tarball any more
- drop optional python2

* Sat Jul 07 2018 Raphael Groner <projects.rg@smart.ms> - 3.7.1-1
- new version

* Tue Jun 19 2018 Miro Hrončok <mhroncok@redhat.com> - 3.7.0-4
- Rebuilt for Python 3.7

* Fri Feb 09 2018 Fedora Release Engineering <releng@fedoraproject.org> - 3.7.0-3
- Rebuilt for https://fedoraproject.org/wiki/Fedora_28_Mass_Rebuild

* Sat Sep 02 2017 Raphael Groner <projects.rg@smart.ms> - 3.7.0-2
- drop precompiled jar files
- be more verbose about tests
- add javadoc subpackage
- move interpreter script into python3 subpackage
- add release_notes folder to documentation
- handle readme file properly

* Tue Aug 15 2017 Raphael Groner <projects.rg@smart.ms> - 3.7.0-1
- initial
