# idle-champions-chest-combinations
Collects Idle Champions of the Forgotten Realms chest combinations from https://idlechampions.fandom.com/wiki/Combinations

# Create the jar file

```
just jar
```

# Deployment
- copy jar to server
- create a state file
- update systemd service file with paths
- enable systemd timer

# Example execution

```
java -cp idle-champions.jar clojure.main -m cli --state-file state.txt --mqtt-uri tcp://mqtt-server:1883
```
