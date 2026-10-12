import network
import urequests
SSID = "SSID"
PASS = "PASS"
BASE_URL = "http://ip:port"
POST_INTERFACE = "/post"
# enable station interface and connect to WiFi access point
print("connect to wifi")
nic = network.WLAN(network.STA_IF)
nic.active(True)
nic.connect(SSID, PASS)
while not nic.isconnected():
    pass
print('network config:', nic.ifconfig())
# send post
print("run post")
response = urequests.post(f"{BASE_URL}{POST_INTERFACE}", json={})
print(response.text)



