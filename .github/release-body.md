A cycle tracker for the Mudita Kompakt. Written from scratch.

## How a period is recorded

Every day is confirmed by hand. Nothing is filled in on your behalf, and nothing ends a
period — you confirm each day it is still going, and when you stop confirming it stops on
its own two days later. A day confirmed late still joins the period rather than starting a
new one, so a forgotten tap costs nothing.

A day the app filled in would be a day it invented, sitting in the record looking exactly
like a day you reported. That is the whole reason it does not.

The forecast comes from the median of the last three cycles rather than the mean, so one
unusually long cycle does not drag the prediction past anything that has actually happened.

## What it needs

Nothing. There are no permissions, and there is no `INTERNET` line in the manifest, so what
is recorded here has no mechanism by which to leave the phone.

The whole record can be written out as one plain text file and read back in. That file is
the only way anything here leaves, and you are the one who moves it.

Requires Android 9 or later.

## The licence

GPL-3.0-only — version 3, not "or later": nothing here can be moved onto a licence that has
not been written yet.

Nothing forced that choice. This is not a fork, and every dependency is permissive. It is
copyleft because of what the app holds: a permissive licence would let someone ship this
same code with telemetry added and never publish a line of it. Copyleft is the part of the
design that survives the code leaving here.

Lato keeps its own licence, SIL Open Font License 1.1.
