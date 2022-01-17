# Publish-Subscribe-Protocol
This assignment is focused on learning about protocol development and the information that is kept in a header to support functionality of a protocol. The aim for this protocol is to provide a publish/subscribe mechanism for processes based on UDP datagrams. We have 2 minimum requirements for this assignment.
1. The issuing of sensor data to a number of subscribers (dashboard subscribes to a broker, and sensors publish their data).
2. The issuing of instructions to actuators (actuators subscribe to a broker, and a dash- board sends instructions to the actuators).
The communication between the processes is realized using UDP sockets and Datagrams. My implementation is written in Java and it can be run on Docker, meaning that my protocol can connect components located at a number of hosts.

<div align="center">
<img width="400" alt="Screenshot 2022-01-17 at 00 51 13" src="https://user-images.githubusercontent.com/34750736/149685222-714d38bd-0003-43f6-b55a-ef70cfa2aad2.png">
<img width="400" alt="Screenshot 2022-01-17 at 00 51 28" src="https://user-images.githubusercontent.com/34750736/149685228-ce834bd2-a2dd-484a-b7d0-899e25635e87.png">
</div>
