call neo4j stop
call rmdir /s/q D:\neo4j-community-3.5.5\data\databases\graph.db
call neo4j-admin import --mode csv --database graph.db --nodes  "D:\_HhrWorkSpace\Python\neo4j\entity.csv" --relationships "D:\_HhrWorkSpace\Python\neo4j\relationship.csv"
call neo4j start
pause