# GUI Architecure

## Introduction
The GUI must be able to retrieve alarms and monitoring information from the Backstage Database (BSDB) and display them to the users.

### Specific consideration
Specifically, the whole system must be able to perform the following tasks:
1. Receive alarms from the IAS Core and display them to users
2. Allow users to take actions on the alarms, such as:
  - **Acknowledgement:** recognize the alarm and identify a course of action that will lead to its fix
  - **Shelving:** mark the alarm as "seen", in order to avoid confusion upon the triggering of new alarms. This is not an acknowledgement because the user does not specify a course of action to be taken. In case a user shelves an alarm, the "shelved" state should have a timeout after which the alarms is "un-shelved"
3. Make automatic acknowledgement (and possibly shelving) of alarms according to their dependencies. This is information is stored in the Configuration Database and should be obtained from there.

The following sections present a an architecture proposal for the GUIs. They represent an evolving architecture and its main components. This document is NOT intended to be a constraint for the development, but merely a guide and support.

## Architecture
The design of this solution is presented considering 3 logical layers:
- **IAS GUI:** a set of Graphical User Interfaces (GUI) whose purpose is to display alarms to end-users and enable them to take specific actions, such as acknowledgement, shelving, etc.

- **IAS WebServer:** a backend to serve the IAS GUI (frontend), whose purpose is to receive alarms from the IAS Core, enrich them with state information, such as acknowledgement state, store them in a Database and send them to IAS GUI.

- **IAS Core:** whose purpose is to get, process and send the alarms from the different subsystems to the IAS WebServer.

This design is summarized in the following diagram:

![Basic Diagram](img/basic_component_diagram.png)

### IASWebServer (Backend)

#### Actions:
The ***IASWebServer*** is the backend of the ***IASGUIs***. As discussed in the introduction, this component needs to:
- Receive alarms from the core, send them to the ***IASGUI*** and store them in a local Database
- Receive acknowledgement and other actions from the ***IASGUI***, update the state of the alarms in the local database and send changes or log info to the to the **LTDB** of the ***IASCore***
- Retrieve configuration information from the ***IASCore***, in order to auto-acknowledge alarms according to their dependencies and composition (which is stored in the Configuration Database of the ***IASCore***)

#### Implementation:
The ***IASWebServer*** will be implemented using the Java Spring framework with a Tomcat Server. The Observers pattern is considered for the design, in order to parallelize sending alarms to the ***IASGUI*** and storing them in the ***IASWebServer*** local database.

#### Architecture:
The following diagram shows a more detailed (conceptual) view of the architecture of the ***IASWebServer***. Some parts of the ***IASCore*** are also detailed for easier understanding of the architecture. Please note that the name of most interfaces (at this moment) is just for explanation purposes, and thus, it is not definitive.

![IASWebServer Components Diagram](img/IASWebserver_component_diagram.png)



This architecture is based on the Model-View-Controller (MVC) design pattern. The model is handled by the ***IASManager***, while actions are triggered by 3 different controllers: ***GUIObserverController***, ***APIController*** and ***AlarmReceiver***. The views correspond to the ***IASGUI***.

The purpose of each of these subcomponent is detailed below:
* **AlarmReceiver:** receives alarms sent from the ***IASCore*** through its ***AlarmSender*** (this is just a preliminary name to identify this component, it will be properly renamed later). ***AlarmReceiver*** is a thin controller that receives the alarms and send them to the ***IASManager***.

* **IASManager:** is the central component of the Web Server. It contains all the model logic and interacts with the other systems through specific controllers. When it receives an alarm from the ***AlarmReceiver*** it notifies ***GUIObserverController*** and ***DatabaseObserver***, who act in parallel.

* **GUIObserverController:** receives alarms from ***IASManager*** and redirects them to the ***IASGUI***.

* **DatabaseObserver:** receives alarms from ***IASManager*** and writes them to a local database.

* **APIController:** when a user applies an action in the ***IASGUI***, for instance it acknowledges an alarm, the action is sent to the ***APIController*** which in turn notifies the ***IASManager*** accordingly.

* **ConfigRetriever:** when the state of an alarm must change due to a users action (e.g. acknowledgement) there may be some other alarms that need to be updated as well. This depends on the alarm dependencies stored in the Configuration Database (***CdB***). The ***ConfigRetriever*** component is responsible of getting this information from the ***IASCore*** through its ***ConfigDBReader*** (which is only a conceptual name for reference purposes and will be updated accordingly later)

* **LogSender:** also when a user takes action on an alarm, some information regarding that action may need to be sent to the ***IASCore***'s ***LongTermDatabase*** (***LTDB***). This is done by the ***IASManager*** through the ***LogSender*** component, which connects to the ***IASCore***'s ***LTDBWriter*** (again a name for reference purposes)

It is worth to note, that the Observers design pattern is being considered here. Being ***IASManager*** the Observable which notifies new alarms to its 2 Observers: ***GUIObserverController*** and ***DatabaseObserver***, who react upon notification. This approach allows us to decouple the writing in database and displaying in GUI of alarms. Furthermore, other actions can be added through the same pattern in the future if it is needed.

### IASGUI (Frontend)
The ***IASGUI*** will be implemented using the Angular 4 framework. It will be a web application available through web browsers. The internal architecture of this component is not defined yet. However, in principle, each GUI will be designed as a separate component accessible through a URL as well as in composed views. This modular approach allows us to define different GUIs easily and also allow users to access many of them simultaneously according to their needs.

### Communication Technologies:
For the communication between the ***IASWebServer*** and the ***IASGUI*** two type of communications are being considered:

* **Sending of Alarms:** the ***IASGUI*** must subscribe to the ***IASWebServer*** and react upon receiving alarms and messages. In order to implement this we consider Server Sent Events, as they provide specific functionality for this purpose. This way, the ***GUIObserverController*** will send events (alarms) to the ***IASGUI***

* **Actions by users:** the ***IASGUI*** must send actions to the ***IASWebServer*** in order to update the state of alarms accordingly. In order to implement this we consider an API Rest, provided by ***APIController*** of the ***IASWebServer***.

## Resources
### Diagrams (Links to edit, for users with permissions only)
[Basic Diagram](https://drive.google.com/open?id=0Bx4VmHTx71pKcUlnZ1lrR0QzOVk)

[Component Diagram](https://drive.google.com/open?id=0Bx4VmHTx71pKMUVhM0RDV2RvWUE)

[Class Diagram](https://drive.google.com/open?id=0B7KJMlIvS-KBT3ZRaG1pamNEMmM)
