# VGAP Rating System

Replacement [Rating System](http://help.planets.nu/Ladder) for [Planets Nu](http://planets.nu).

## Setup

### Installing Boot.clj

https://github.com/boot-clj/boot

```
wget https://github.com/boot-clj/boot/releases/download/2.2.0/boot.sh
mv boot.sh boot
chmod a+x boot
sudo mv boot /usr/local/bin/
boot -u
boot -h
```

### Optimize JVM settings for Clojure

Create ~/.bash_profile with these contents (or at end if already exists):

```
export BOOT_JVM_OPTIONS='-client -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xmx2g -XX:MaxPermSize=128m -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xverify:none'
```

source ~/.bash_profile

### Download and test project

```
git clone git@github.com:ericlavigne/planets-rating.git
cd planets-rating
cp settings.clj.example settings.clj
(Modify settings.clj to contain your real AWS credentials.)
boot test
lein test vgap.turn-file-test
```

