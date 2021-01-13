### 1. Project Description

Implement a mutual exclusion service among n processes using Lamport’s distributed mutual exclusion algorithm.

### 2. Include Files

* launcher.sh -- The shell script of lauching this program
* cleanup.sh -- The shell script of killing this program process
* sources files
  * Listener
  * Message
  * Node
  * Tester
* libs folder
  * commons-math3-3.6.1.jar

### 3. How to Run it

1. Open shell and use SSH Connecting to <b>csgrads1</b> server

```shell
ssh -Y user@csgrads1.utdallas.edu
```

Make new directory and Move these file to <b>csgrads1</b> server

To compile source code,  go to the directory of project folder and compile it using following command:

```shell
javac -Djava.ext.dirs=libs *.java
```

​	<b>Importance</b>:

​		a. <b>Node</b> is the main class for this program.

​		b. On csgrads1 server, <b>Set up for password less login</b> to dcmachines

2. Modified shell script locally according to your configuration

3. change mode of shell script file

   ```shell
   chmod 777 launcher.sh
   chmod 777 cleanup.sh
   ```

4. Run launcher.sh on <b>csgrads1</b> server

```shell
./launcher.sh
```

5. Run program to check the result

```
java Tester
```

6. Run cleanup.sh on csgrads1 server

```shell
./cleanup.sh
```
