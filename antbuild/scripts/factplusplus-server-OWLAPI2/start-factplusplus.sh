if [ "$(java -version 2>&1 | grep 64-Bit)" == "" ]; then
  REASONERLIB=lib32
else
  REASONERLIB=lib64
fi
for f in ../lib/*.jar
do
  CLASSPATH=$CLASSPATH:$f
done
for f in $REASONERLIB/*.jar
do
  CLASSPATH=$CLASSPATH:$f
done
java -Djava.library.path=$REASONERLIB -cp ../owllink-bin.jar:../lib/owlapi-bin.jar:$CLASSPATH org.semanticweb.owlapi.owllink.server.serverfactory.FaCTLegacyServerFactory -port 8080
