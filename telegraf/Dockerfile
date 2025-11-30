FROM telegraf:1.35.2

USER root

COPY telegraf/custom-entrypoint.sh /custom-entrypoint.sh
COPY telegraf/telegraf.conf /telegraf.conf
RUN chmod +x /custom-entrypoint.sh

ENTRYPOINT ["/custom-entrypoint.sh"]