# idle-champions-chest-combinations
Collects Idle Champions of the Forgotten Realms chest combinations

## Sources

- https://idlechampions.fandom.com/wiki/Combinations
- https://incendar.com/idlechampions_codes.php

# Create the jar file

```
just jar
```

# Deployment
- copy jar to server
- create an empty state file: `touch state.txt`.
- update systemd service file with paths
- enable systemd timer

# Example execution

```
java -cp idle-champions.jar clojure.main -m cli --state-file state.txt --mqtt-uri tcp://mqtt-server:1883
```
