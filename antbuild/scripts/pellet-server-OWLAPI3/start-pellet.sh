for f in lib/*.jar
do
  CLASSPATH=$CLASSPATH:$f
done
for f in ../lib/*.jar
do
  CLASSPATH=$CLASSPATH:$f
done
java -cp .:../owllink-bin.jar:../lib/owlapi-bin.jar:$CLASSPATH org.semanticweb.owlapi.owllink.server.serverfactory.PelletServerFactory -port 8080
        