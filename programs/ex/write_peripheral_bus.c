#include <stdint.h>
#include <flexpret_io.h>

// This simple program is to see whether we are able to write some data
//  to the peripheral bus (io.bus) of the flexPRET

#define PERIPHERAL_BUS_START 0x40000000UL

int main() {
    volatile unsigned int *p;
    p = PERIPHERAL_BUS_START;
    *p = 7;
    p++;
    *p=8;

    _fp_print(888168);

    _fp_finish();
    while(1) {}
    // Not strictly required; just wanted to let the compiler know.
    __builtin_unreachable();
}


