This document contains the specifications of the returned JSON documents.

# JSON documents containing a graph
## Methods:
- `/graphLocalFile`
- `/graphRemoteFile`
- `/expandNode`
- `get-cuurent-graph`

## Format
The returned JSON document is formatted as ELK-JSON. The exact specification may be found
in [the ELK documentation](https://eclipse.dev/elk/documentation/tooldevelopers/graphdatastructure/jsonformat.html). The
following options are used in the generation of the document:

- `omitZeroDimension` &#x2192; Whenever no dimension is specified for a given element, the receiving program should
  assume that it is `0` instead
- `omitZeroPosition` &#x2192; Whenever no position is specified for a given element, the receiving program should assume
  that it is `0` instead
- `shortLayoutOptionKeys` &#x2192; All layout options are shortened to reduce file size

The document contains some, if not all the options described [here](/docs/EXTRA-LAYOUT-OPTIONS.md) in addition to the layout options included in ELK.

# JSON document mapping signal names to bitindices
## Methods:
- `/get-net-information`

## Format:
The JSON document is formatted as follows:

```
{
  "signals": {
    "<signalname>": {
      "scope": "<hdlname|scopename>",
      "bits": [
        <bitindex>,
        ...
      ]
    },
    ...
  }
}
```