FROM curlimages/curl:8.10.1

USER ROOT

# Copy setup scripts from context root (telegraf/ in sens2vizapp)
COPY grafana-telegraf-setup/grafana_setup.sh /grafana_setup.sh

# Ensure scripts are executable
RUN chmod +x /grafana_setup.sh

# Return to non-root user for security (curlimages default user 1001)
USER 1001

ENTRYPOINT ["/bin/sh", "-c", "/grafana_setup.sh"]
