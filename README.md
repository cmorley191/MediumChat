# MediumChat

MediumChat is a group communication application that implements
the 
[Medium Peer Network Engine](https://github.com/cmorley191/MediumPeerNetworkEngine).

The application uses the UDP implementation of the engine's basic
features for communication. Users can connect and talk to other 
users directly.

Planned development includes common chat program features (mainly
aesthetics), GUI, and more. MediumChat will most likely keep pace
with new features added to the engine, as well.

## Installation

This directory is an 
[Eclipse Mars IDE](https://projects.eclipse.org/releases/mars) 
Java project. The source files in `src` may be used in any Java
development space without need for any extra procedure. Usage in
Eclipse is best done by using File...Import...

The project requires the Medium Peer Network Engine. Make sure 
the project's files have access to it. In Eclipse, this is best done
by installing the MPNE Eclipse project from the 
[GitHub Page](https://github.com/cmorley191/MediumPeerNetworkEngine)
and setting it as a project dependency in this project's build path.
(Project...Properties...Java Build Path...)

## Usage

Compile the project and run `MPNEClient`. User interaction is done
through the console.

## Contributing

Fork the repository and create a pull request to implement new
features.

## History

  * 2016-2-19 Import from MPNE

## Credits

Developed by Charlie Morley

## License

Copyright (c) 2016 Charlie Morley Some Rights Reserved  
CC BY  
cmorley191@gmail.com

This software's binary and source code are released under the 
Creative Commons Attribution license.  
[http://creativecommons.org/licenses/by/4.0/](http://creativecommons.org/licenses/by/4.0/)