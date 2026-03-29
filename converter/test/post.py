import requests

with open("test3.mid", "rb") as f:
    r = requests.post("http://127.0.0.1:5000/mid_to_text", data=f.read())
    print(r.text)