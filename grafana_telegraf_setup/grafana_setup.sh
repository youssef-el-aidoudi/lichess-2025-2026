#!/bin/sh

GRAFANA_URL=${GF_URL}
DATASOURCE_NAME=${TELEGRAF_DB}
DATASOURCE_URL=${POSTGRES_URL}
DATASOURCE_USER=${TELEGRAF_USER}
DATASOURCE_PASSWORD=${TELEGRAF_PASSWORD}
DATASOURCE_DB=${TELEGRAF_DB}
DASHBOARD_FILE="/system_metrics_dashboard.json"

wait_for_grafana() {
  echo "Waiting for Grafana to be ready..."
  while ! curl -s "http://grafana:3000/api/health" | grep '"database": "ok"' > /dev/null; do
    echo "Waiting for Grafana to start..."; sleep 5;
  done
  echo "Grafana is up!"
}

create_datasource_if_not_exists() {
  echo "Checking if datasource exists..."
  if curl -s "$GRAFANA_URL/api/datasources/name/$DATASOURCE_NAME" | grep '"message":"Data source not found"' > /dev/null; then
    echo "Datasource not found. Creating datasource..."
    curl -X POST "$GRAFANA_URL/api/datasources" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "'"${DATASOURCE_NAME}"'",
        "type": "postgres",
        "access": "proxy",
        "url": "'"${DATASOURCE_URL}"'",
        "user": "'"${DATASOURCE_USER}"'",
        "secureJsonData": {
          "password": "'"${DATASOURCE_PASSWORD}"'"
        },
        "jsonData": {
            "sslmode": "disable",
            "timescaledb": true
        },
        "database": "'"${DATASOURCE_DB}"'",
        "basicAuth": false,
        "isDefault": true,
        "withCredentials": false
      }'
    echo "Datasource created successfully."

   # Récupère l'UID du datasource nouvellement créé
    DATASOURCE_UID=$(curl -s "$GRAFANA_URL/api/datasources/name/$DATASOURCE_NAME" | grep -o '"uid":"[^"]*"' | sed 's/"uid":"\([^"]*\)"/\1/')
    echo "Datasource UID: $DATASOURCE_UID"

  else
    echo "Datasource already exists."

    # Récupère l'UID du datasource existant
    DATASOURCE_UID=$(curl -s "$GRAFANA_URL/api/datasources/name/$DATASOURCE_NAME" | grep -o '"uid":"[^"]*"' | sed 's/"uid":"\([^"]*\)"/\1/')
    echo "Datasource UID: $DATASOURCE_UID"
  fi

      export DS_TELEGRAF="$DATASOURCE_UID"

}

import_dashboard() {
  sed 's/\${DS_TELEGRAF}/'"$DS_TELEGRAF"'/g' $DASHBOARD_FILE > /tmp$DASHBOARD_FILE

  echo "Importing dashboard..."
  curl -X POST "$GRAFANA_URL/api/dashboards/db" \
    -H "Content-Type: application/json" \
      --data-binary @/tmp$DASHBOARD_FILE

  echo "Dashboard imported successfully."
}

set_active_org() {
  echo "Setting active organization to Org ID: $GF_MAIN_ORG_ID"
  curl -X POST "$GRAFANA_URL/api/user/using/$GF_MAIN_ORG_ID" \
    -H "Content-Type: application/json" \
    -d '{}'
  echo "Organization set to $GF_MAIN_ORG_ID"
}

create_folder() {
  FOLDER_UID=$(curl -s "$GRAFANA_URL/api/search?query=System%20Alerts&type=dash-folder" \
    | grep -o '"uid":"[^"]*' | grep -o '[^"]*$')

  if [ -z "$FOLDER_UID" ]; then
    echo "Folder does not exist. Creating folder..."
    FOLDER_UID=$(curl -s -X POST "$GRAFANA_URL/api/folders" \
      -H "Content-Type: application/json" \
      -d '{
        "title": "System Alerts"
      }' | grep -o '"uid":"[^"]*"' | sed 's/"uid":"\([^"]*\)"/\1/')
    echo "Folder created with UID: $FOLDER_UID"
    export FOLDER_UID="$FOLDER_UID"

  else
    echo "Folder already exists with UID: $FOLDER_UID"
    export FOLDER_UID="$FOLDER_UID"
  fi
}


#set_alerts() {
#cat <<EOF > alert_rule.json
#{
#  "title": "CPU Usage Alert",
#  "ruleGroup": "API Group",
#  "folderUID": "$FOLDER_UID",
#  "noDataState": "NoData",
#  "execErrState": "Error",
#  "for": "2m",
#  "condition": "C",
#  "annotations": {
#    "description": "",
#    "runbook_url": "",
#    "summary": ""
#  },
#  "labels": {
#    "": ""
#  },
#  "isPaused": false,
#  "notification_settings": {
#    "receiver": "grafana-default-email"
#  },
#  "data": [
#    {
#      "refId": "A",
#      "relativeTimeRange": {
#        "from": 600,
#        "to": 0
#      },
#      "datasourceUid": "$DS_TELEGRAF",
#      "model": {
#        "editorMode": "builder",
#        "format": "table",
#        "intervalMs": 1000,
#        "maxDataPoints": 43200,
#        "rawSql": "SELECT AVG(usage_system) FROM cpu LIMIT 50",
#        "refId": "A",
#        "sql": {
#          "columns": [
#            {
#              "name": "AVG",
#              "parameters": [
#                {
#                  "name": "usage_system",
#                  "type": "functionParameter"
#                }
#              ],
#              "type": "function"
#            }
#          ],
#          "groupBy": [
#            {
#              "property": {
#                "type": "string"
#              },
#              "type": "groupBy"
#            }
#          ],
#          "limit": 50
#        },
#        "table": "cpu"
#      }
#    },
#    {
#      "refId": "C",
#      "relativeTimeRange": {
#        "from": 600,
#        "to": 0
#      },
#      "datasourceUid": "__expr__",
#      "model": {
#        "conditions": [
#          {
#            "evaluator": {
#              "params": [
#                80
#              ],
#              "type": "gt"
#            },
#            "operator": {
#              "type": "and"
#            },
#            "query": {
#              "params": [
#                "C"
#              ]
#            },
#            "reducer": {
#              "params": [],
#              "type": "last"
#            },
#            "type": "query"
#          }
#        ],
#        "datasource": {
#          "type": "__expr__",
#          "uid": "__expr__"
#        },
#        "expression": "A",
#        "intervalMs": 1000,
#        "maxDataPoints": 43200,
#        "refId": "C",
#        "type": "threshold"
#      }
#    }
#  ]
#}
#EOF
#
#curl -X POST "$GRAFANA_URL/api/v1/provisioning/alert-rules" \
#  -H "Content-Type: application/json" \
#  --data-binary @alert_rule.json
#
## Supprimer le fichier temporaire
#rm alert_rule.json
#
#}

# Exécuter les fonctions
wait_for_grafana
set_active_org
create_datasource_if_not_exists
#set_alerts