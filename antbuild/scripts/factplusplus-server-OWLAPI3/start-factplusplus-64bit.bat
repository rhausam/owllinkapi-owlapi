@echo off
setLocal EnableDelayedExpansion
set MyCLASSPATH=%CLASSPATH%
set REASONERLIB=lib64
for /R %REASONERLIB% %%a in (*.jar) do (
   set MyCLASSPATH=!MyCLASSPATH!;%%a
)
for /R ../lib %%a in (*.jar) do (
   set MyCLASSPATH=!MyCLASSPATH!;%%a
)
java -Djava.library.path=%REASONERLIB% -cp .;../owllink-bin.jar;../lib/owlapi-bin.jar;"%MyCLASSPATH%" org.semanticweb.owlapi.owllink.server.serverfactory.FaCTServerFactory -port 8080
set MyClassPath=
