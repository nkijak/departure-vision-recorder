from flask import Flask, request
from kafka import KafkaProducer
from kafka.errors import KafkaError
import os
from waitress import serve

app = Flask(__name__)

broker=os.environ.get('KAFKA', '192.168.1.211:9092')
print(f"broker at {broker}")
p = KafkaProducer(bootstrap_servers=broker)


@app.route('/topic/<path:path>', methods=['POST'])
def catch_all(path):
    key = request.args.get('key')
    if key:
        result = p.send(path, key=bytearray(key, 'utf8'), value=request.data)
    else:
        result = p.send(path, request.data)
    p.flush()
    try:
        result.get(timeout=10)
        print('topic: {}, key: {}, data:{}'.format(path, key, request.data))
        return 'topic: {}, key: {}, data:{}'.format(path, key, request.data)
    except KafkaError as ke:
        print('ERROR topic: {}, key: {}, data:{}'.format(path, key, request.data))
        raise ke 

@app.route('/health', methods=['GET'])
def health():
    return 'UP'

if __name__ == '__main__':
    serve(app, listen='*:5000')
