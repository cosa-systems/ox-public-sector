Name:           open-xchange-appsuite-public-sector
Version:        @OXVERSION@
%define         ox_release 0
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
Vendor:         Open-Xchange
URL:            http://open-xchange.com
Packager:       Viktor Pracht <viktor.pracht@open-xchange.com>
License:        AGPLv3+
Summary:        UI customizations for the public sector
Autoreqprov:    no
Source:         %{name}_%{version}.orig.tar.bz2

BuildArch:      noarch
BuildRoot:      %{_tmppath}/%{name}-%{version}-root
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires:  ant
%else
BuildRequires:  ant-nodeps
%endif
%if 0%{?suse_version}
BuildRequires:  nodejs >= 10.0
BuildRequires:  npm >= 6.0
BuildRequires:  java-1_8_0-openjdk-devel
%else
BuildRequires:  nodejs >= 10.0
BuildRequires:  java-1.8.0-openjdk-devel
%endif
Requires(post): open-xchange-appsuite-manifest >= @OXVERSION@, open-xchange-appsuite-manifest < @NEXTMINOR@

%description
UI customizations for the public sector

%if 0%{?rhel_version} || 0%{?fedora_version}
%define docroot /var/www/html/
%else
%define docroot /srv/www/htdocs/
%endif

%package        static
Group:          Applications/Productivity
Summary:        UI customizations for the public sector
Autoreqprov:    no
Requires:       open-xchange-appsuite >= @OXVERSION@, open-xchange-appsuite < @NEXTMINOR@

%description    static
UI customizations for the public sector

This package contains the static files.

%prep

%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -Dhtdoc=%{docroot} -DkeepCache=true -f build/build.xml build

%clean
%{__rm} -rf %{buildroot}

%define update /opt/open-xchange/appsuite/share/update-themes.sh

%post
if [ $1 -eq 1 -a -x %{update} ]; then %{update} --later; fi

%postun
if [ -x %{update} ]; then %{update} --later; fi

%files
%defattr(-,root,root)
%doc SourceCodeOrigin.txt
%dir /opt/open-xchange
%dir /opt/open-xchange/appsuite
/opt/open-xchange/appsuite
%dir /opt/open-xchange/etc
%dir /opt/open-xchange/etc/settings
%config(noreplace) /opt/open-xchange/etc/settings/*

%files static
%defattr(-,root,root)
%doc SourceCodeOrigin.txt
%dir %{docroot}/appsuite
%{docroot}/appsuite

%changelog
