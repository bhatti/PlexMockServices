if [ $# -eq 0 ];
then
mockMode=record  # or play 
else
mockMode=$1
fi;
port=8080
echo ""
curl -X POST http://localhost:$port/users -H "XMockMode: $mockMode" -H 'Content-Type: application/json' -d '{ "name": "John Doe"}'
echo "adding..."
curl -H 'Content-Type: application/json' -H "XMockMode: $mockMode" http://localhost:$port/users/0
echo "updating..."
curl -X PUT http://localhost:$port/users/0 -H "XMockMode: $mockMode" -H 'Content-Type: application/json' -d '{ "name": "Jane Doe", "age": 30, "id": 0}'
echo ""
#echo "updating phone"
#curl -X PUT http://localhost:$port/users/0/params_phone -H "XMockMode: $mockMode" --data 'phone=800-800-8000&email=email'
echo "listing..."
echo ""
curl -H 'Content-Type: application/json' -H "XMockMode: $mockMode" http://localhost:$port
echo ""
curl -X DELETE http://localhost:$port/users/0 -H "XMockMode: $mockMode" -H 'Content-Type: application/json'
echo ""
curl -H 'Content-Type: application/json' -H "XMockMode: $mockMode" http://localhost:$port
echo ""

#echo "non-existing"
#curl -X PUT http://localhost:$port/users/0/non-existing -H "XMockMode: $mockMode" --data 'phone=800-800-8000&email=email'
