FROM nunopreguica/sd2526tpbase

WORKDIR /home/sd

ADD hibernate.cfg.xml .
ADD messages.props .

COPY tls/ /home/sd/tls/
COPY target/sd2526-tp2-1.jar sd2526.jar
RUN ln -s /home/sd/tls /tls

ENV JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=/home/sd/tls/truststore.ks \
    -Djavax.net.ssl.trustStorePassword=changeit"