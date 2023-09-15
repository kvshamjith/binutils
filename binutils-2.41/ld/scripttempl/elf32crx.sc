# Linker Script for National Semiconductor's CRX-ELF32.
#
# Copyright (C) 2014-2023 Free Software Foundation, Inc.
#
# Copying and distribution of this file, with or without modification,
# are permitted in any medium without royalty provided the copyright
# notice and this notice are preserved.

# Using an empty script for ld -r is better than mashing together
# sections.  This hack likely leaves ld -Ur broken.
test -n "${RELOCATING}" || exit 0

# The next line should be uncommented if it is desired to link
# without libstart.o and directly enter main.

# ENTRY=_main

test -z "$ENTRY" && ENTRY=_start
cat <<EOF

/* Example Linker Script for linking NS CRX elf32 files.

   Copyright (C) 2014-2023 Free Software Foundation, Inc.

   Copying and distribution of this script, with or without modification,
   are permitted in any medium without royalty provided the copyright
   notice and this notice are preserved.  */


OUTPUT_FORMAT("${OUTPUT_FORMAT}")
OUTPUT_ARCH(${ARCH})
${RELOCATING+ENTRY(${ENTRY})}

/* Define memory regions.  */
MEMORY
{
	rom	    : ORIGIN = 0x2,	    LENGTH = 3M
	ram	    : ORIGIN = 4M,	    LENGTH = 10M
}

/*  Many sections come in three flavours.  There is the 'real' section,
    like ".data".  Then there are the per-procedure or per-variable
    sections, generated by -ffunction-sections and -fdata-sections in GCC,
    and useful for --gc-sections, which for a variable "foo" might be
    ".data.foo".  Then there are the linkonce sections, for which the linker
    eliminates duplicates, which are named like ".gnu.linkonce.d.foo".
    The exact correspondences are:

    Section	Linkonce section
    .text	.gnu.linkonce.t.foo
    .rdata	.gnu.linkonce.r.foo
    .data	.gnu.linkonce.d.foo
    .bss	.gnu.linkonce.b.foo
    .debug_info	.gnu.linkonce.wi.foo  */

SECTIONS
{
  .init :
  {
    __INIT_START = .;
    KEEP (*(SORT_NONE(.init)))
    __INIT_END = .;
  } > rom

  .fini :
  {
    __FINI_START = .;
    KEEP (*(SORT_NONE(.fini)))
    __FINI_END = .;
  } > rom

  .jcr :
  {
    KEEP (*(.jcr))
  } > rom

  .text :
  {
    __TEXT_START = .;
    *(.text) *(.text.*) *(.gnu.linkonce.t.*)
    __TEXT_END = .;
  } > rom

  .rdata :
  {
    __RDATA_START = .;
    *(.rdata_4) *(.rdata_2) *(.rdata_1) *(.rdata.*) *(.gnu.linkonce.r.*) *(.rodata.*)
    __RDATA_END = .;
  } > rom

  .ctor ALIGN(4) :
  {
    __CTOR_START = .;
    /* The compiler uses crtbegin.o to find the start
       of the constructors, so we make sure it is
       first.  Because this is a wildcard, it
       doesn't matter if the user does not
       actually link against crtbegin.o; the
       linker won't look for a file to match a
       wildcard.  The wildcard also means that it
       doesn't matter which directory crtbegin.o
       is in.  */

    KEEP (*crtbegin.o(.ctors))
    KEEP (*crtbegin?.o(.ctors))

    /* We don't want to include the .ctor section from
       the crtend.o file until after the sorted ctors.
       The .ctor section from the crtend file contains the
       end of ctors marker and it must be last */

    KEEP (*(EXCLUDE_FILE (*crtend.o *crtend?.o) .ctors))
    KEEP (*(SORT(.ctors.*)))
    KEEP (*(.ctors))
    __CTOR_END = .;
  } > rom

  .dtor ALIGN(4) :
  {
    __DTOR_START = .;
    KEEP (*crtbegin.o(.dtors))
    KEEP (*crtbegin?.o(.dtors))
    KEEP (*(EXCLUDE_FILE (*crtend.o *crtend?.o) .dtors))
    KEEP (*(SORT(.dtors.*)))
    KEEP (*(.dtors))
    __DTOR_END = .;
  } > rom

  .data :
  {
    __DATA_START = .;
    *(.data_4) *(.data_2) *(.data_1) *(.data) *(.data.*) *(.gnu.linkonce.d.*)
    __DATA_END = .;
  } > ram AT > rom

  .bss (NOLOAD) :
  {
    __BSS_START = .;
    *(.bss_4) *(.bss_2) *(.bss_1) *(.bss) *(COMMON) *(.bss.*) *(.gnu.linkonce.b.*)
    __BSS_END = .;
  } > ram

/* You may change the sizes of the following sections to fit the actual
   size your program requires.

   The heap and stack are aligned to the bus width, as a speed optimization
   for accessing data located there.  */

  .heap (NOLOAD) :
  {
    . = ALIGN(4);
    __HEAP_START = .;
    . += 0x2000; __HEAP_MAX = .;
  } > ram

  .stack (NOLOAD) :
  {
    . = ALIGN(4);
    . += 0x6000;
    __STACK_START = .;
  } > ram

  .istack (NOLOAD) :
  {
    . = ALIGN(4);
    . += 0x100;
    __ISTACK_START = .;
  } > ram

EOF

source_sh $srcdir/scripttempl/misc-sections.sc
source_sh $srcdir/scripttempl/DWARF.sc

cat <<EOF
}

__DATA_IMAGE_START = LOADADDR(.data);
EOF
