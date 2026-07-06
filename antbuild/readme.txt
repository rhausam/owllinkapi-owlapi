----------------------------------
OWLlink API - in short
----------------------------------

The OWLlink API is both a client API and a server API.

The client API enables OWLAPI-based application to use OWLlink services offered by an arbitrary OWLlink server that
provides a HTTP/XML binding (e.g., RacerPro)
    () Flexible and extensible API for defining OWLlink requests/responses
    () All core OWLlink requests and responses are supported by default
    () Fast support for the HTTP/XML binding of OWLlink
    () Full OWLAPI integration: OWLReasoner implementation and specific reasoner interfaces


The server API enables OWLlink-unaware OWL reasoners such as FaCT++ or Pellet to run as OWLlink servers. For
reasoner implementors it also provide a rich API that can be used to enhance reasoners with OWLlink functionality.
    () Use OWLlink-unaware OWL reasoners or older versions of them as OWLlink server
    () Out-of-the-box factories and scripts for running Pellet and FaCT++ as OWLlink servers
    () Bridge between OWLAPI v2 reasoners and OWLAPI v3 reasoners
    () Framework for reasoner developers to enhance their reasoners with OWLlink functionality (parsers and renderers)


-------------------
OWLlink client API
-------------------


To use the client API of the  OWLlink API in your application add "owllink-bin.jar" to the classpath of your application.
Make sure that you also add the following libraries (see "lib" folder):
   () owlapi-distribution-4.5.29.jar - The OWL API 4.5
   () commons-logging-1.1.1.jar
   () junit.jar
Then you can start any native OWLlink server (e.g. RacerPro) or start an OWLlink server wrapping any OWLReasoner via the
OWLlink server API as described in the following.


-------------------
OWLlink server API
-------------------


To use the server API of the OWLlink API add also the following libraries (see "lib" folder) to the classpath:
   ()javax.servlet.jar
   () org.morthbay.jetty.jar
   () org.morthbay.jmx.jar


To run Pellet ( (c) by Clark&Parsia ) out-of-the box as OWLlink server:

(1) Pellet version 1.5.x and 2.x via OWL API 2.x
    Copy the pellet libraries into the folder "pellet-server-OWLAPI2/lib" and run the start-pellet script.
    The OWLlink server component will start at localhost:8080
(2) Pellet v. 2.x via OWL API 3.0
    Copy the pellet libraries into the folder "pellet-server-OWLAPI3/lib" and run the start-pellet script.
    The OWLlink server component will start at localhost:8080
    Please make sure that Pellet 2.x supports the official OWL API 3. There are versions such as Pellet 2.0.1 that
    support only a previous unstable version of OWL API 3.


To run FaCTPlusPlus ( (c) by University of Manchester, England) out-of-the-box as OWLlink server:


(1) FaCTPlusPlus for OWLAPI 2.x
    Copy the FaCTPlusPlus jar file and the native shared libraries for 32bit into the folder
    "factplusplus-server-OWLAPI2/lib32" and those for 64bit into "factplusplus-server-OWLAPI2/lib64".
    Make sure, that each of these folders contains the jar file and the native shared library file.

    Using Unix/Linux run the "start-factplusplus.sh" script in "factplusplus-server-OWLAPI2".
    Windows users start the OWLlink server either by running "start-factplusplus-32bit.bat" (for 32bit java)
    or "start-factplusplus-64bit.bat" (for 64bit java)
(2) FaCTPlusPlus for OWLAPI 3.0
    Copy the FaCTPlusPlus jar file and the native shared libraries for 32bit into the folder
    "factplusplus-server-OWLAPI3/lib32" and those for 64bit into "factplusplus-server-OWLAPI3/lib64".
    Make sure, that each of these folders contains the jar file and the native shared library file.

    Using Unix/Linux run the "start-factplusplus.sh" script in "factplusplus-server-OWLAPI3".
    Windows users start the OWLlink server either by running "start-factplusplus-32bit.bat" (for 32bit java)
    or "start-factplusplus-64bit.bat" (for 64bit java)

Tu run HermiT 1.2.x (x > 1) ( (c) by University of Oxford, England) out-of-the-box as OWLlink server:


( ) Copy "HermiT.jar" into the folder "hermit-server/lib" and run the start-hermit script.
    The OWLlink server component will start at localhost:8080


----------------------
OWLlink Protege Plugin
----------------------

To enable OWLlink support in Protege copy the file named "org.semanticweb.owllink.protege.jar" into
the "plugin" folder of the Protege distribution and restart Protege. Note that the OWLlink Protege
Plugin only works with Protege 4.1 or higher.


Further information, documentation and examples can be found at http://owllink-owlapi.sourceforge.net

(C) 2010 by Olaf Noppens