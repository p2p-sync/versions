# Object Management and Versioning
[![Build Status](https://travis-ci.org/p2p-sync/versions.svg?branch=master)](https://travis-ci.org/p2p-sync/versions)
[![Coverage Status](https://coveralls.io/repos/p2p-sync/versions/badge.svg?branch=master&service=github)](https://coveralls.io/github/p2p-sync/versions?branch=master)

# Install
Use Maven to add this component as your dependency:

```xml

<repositories>
  <repository>
    <id>version-mvn-repo</id>
    <url>https://raw.github.com/p2p-sync/versions/mvn-repo/</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
    </snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.rmatil.sync.version</groupId>
    <artifactId>sync-version</artifactId>
    <version>0.1-SNAPSHOT</version>
  </dependency>
</dependencies>

```

# Overview
[![Architectural Overview](https://cdn.rawgit.com/p2p-sync/versions/2d2192873c84878e28be3785f4433421452d6216/src/main/resources/img/architectural-overview.svg)](https://cdn.rawgit.com/p2p-sync/versions/2d2192873c84878e28be3785f4433421452d6216/src/main/resources/img/architectural-overview.svg)

This component provides functionality to maintain versions of files resp. directories in a data structure called
`Object Store`. Additionally, information about their existence on a storage adapter is maintained as well as 
whether the element is shared and with whom. This data is persisted for each element in a `PathObject`. 
Tracked elements are listed in an index file (`index.json`). To avoid building the same directory structure again for storing `PathObjects`, a hash is computed for the path to the file resp. directory. This hash is then stored in the index file along with the path to the file.


See the following two JSON files as example: 

```javascript
// index.json

{
  "paths": {
    "someDir": "71d7bda783b5264550dd268fbda8fe4f47b07481743e4016e8906f1c4b08187a"
  }
}

```

```javascript
// PathObject for someDir 71/d7bda783b5264550dd268fbda8fe4f47b07481743e4016e8906f1c4b08187a/71d7bda783b5264550dd268fbda8fe4f47b07481743e4016e8906f1c4b08187a.json

{
  "name": "someDir",
  "path": "",
  "pathType": "DIRECTORY",
  "accessType": null,
  "isShared": false,
  "deleted": {
    "deleteType": "EXISTENT",
    "deleteHistory": [
      "b750c33f5b1c5ceb6e1ba7d495e34ab0..."
    ]
  },
  "owner": null,
  "sharers": [
    {
      "username": "Jason Response",
      "accessType": "ACCESS_REMOVED",
      "sharingHistory": [
        "a970ec592dd9e24de01727897d04d15c...",
        "e80a0e5e10008d56875248d30d5f5328..."
      ]
    }
  ],
  "versions": [
    {
      "hash": "1147f9e50d0d96c179f0ba46b026d77a..."
    }
  ]
}

```

`PathObjects` are stored in a directory named equally to the file hash. The first two characters of the hash build another directory structure to avoid slow operations on certain filesystems due to too many directories:

> Some filesystems slow down if you put too many files in the same directory; making the first byte of the SHA1 into a directory is an easy way to create a fixed, 256-way partitioning of the namespace for all possible objects with an even distribution
>
> -- <cite>Loeliger, J., & McCullough, M. (2012). Version Control with Git: Powerful Tools and Techniques for Collaborative Software Development.</cite>

## Object Manager
An `ObjectManager` simplifies the access to `PathObjects`. It provides methods to create and remove a `PathObject`. 
Furthermore, utility methods to retrieve the hash for a particular path to a file are specified.
Its interface can be found in [`IObjectManager`](https://github.com/p2p-sync/versions/blob/master/src/main/java/org/rmatil/sync/version/api/IObjectManager.java)

## Sharer Manager
In addition to an `ObjectManager`, the `SharerManager` abstracts the access to sharing-related information. In detail, its
interface [`ISharerManager`](https://github.com/p2p-sync/versions/blob/master/src/main/java/org/rmatil/sync/version/api/ISharerManager.java) specifies methods to modify the list of sharers resp. to set an owner for a particular element.

## Delete Manager
Finally, a `DeleteManager` provides access to information about the existence of a particular element on the storage adapter
to which the `ObjectStore` is linked. Its interface specification is defined in [IDeleteManager](https://github.com/p2p-sync/versions/blob/master/src/main/java/org/rmatil/sync/version/api/IDeleteManager.java)



# License

```
   Copyright 2015 rmatil

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
