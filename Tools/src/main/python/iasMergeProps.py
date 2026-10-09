#! /usr/bin/env python3

"""
Merge two Java property files (e.g., Kafka configuration files) and write
the result to stdout.

Properties from the second file override those from the first.  Properties
unique to the first file are kept in place.  Properties unique to the second
file are appended at the end.  Overridden properties from the first file are
commented out with a '#REPLACED' prefix so the user can see what changed.

Comments and blank lines from the first file are preserved.
"""
import sys
import os
import argparse

def parse_args():
    """
    Parse command-line arguments.

    Returns:
        argparse.Namespace: contains PropFile1, PropFile2, and comment.
    """
    prg_name = os.path.basename(sys.argv[0])
    parser = argparse.ArgumentParser(
        prog=prg_name,
        description='Merge the properties of passed files and send the result in the stdout',
        formatter_class=argparse.RawTextHelpFormatter,
        epilog='''Merge the properties in PropFile1 with those in PropFile2 and writes the result in stdout:
 - Properties in PropFile1 and missing in PropFile2 are are kept untouched
 - Properties in PropFile2 and missing in PropFile1 are added at the end
 - Properties defined in PropFile2 override those in PropFile1
Comments in PropFile1 are preserved

The files of properties must be formatted as java property files''')
    parser.add_argument(
        '-c', '--comment',
        type=str,
        required=False,
        default='',
        help='Comment to add before the proeprties defined in PropFile2')
    parser.add_argument('PropFile1', help='First file of properties')
    parser.add_argument('PropFile2', help='Second file of properties')
    return parser.parse_args()

def parse_line(line: str)-> tuple[str, str, str]:
    """
    Parse a single line from a Java properties file.

    Args:
        line: a raw line from the properties file.

    Returns:
        A tuple (prop_name, prop_value, comment) where comment is the
        trailing '# ...' portion if present, else an empty string.
        For blank or comment-only lines prop_name and prop_value are empty.

    Raises:
        ValueError: if the line contains text that is not a comment and
            does not have a valid 'key=value' format.
    """
    stripped = line.strip()
    if not stripped:
        return ("", "", "")

    comment = ""
    prop_str = "" # prop=vale string
    prop_name= ""
    prop_value = ""
    comment_idx = stripped.find('#')
    if comment_idx>=0:
        comment = stripped[comment_idx:]
        prop_str = stripped[:comment_idx].strip()
    else:
        prop_str = stripped

    if prop_str:
        equal_idx = prop_str.find('=')
        if equal_idx>=0:
            prop_name = prop_str[:equal_idx].strip()
            prop_value = prop_str[equal_idx+1:].strip()         

        if equal_idx<0 or len(prop_name)==0 or len(prop_value)==0:
            raise ValueError(f"Malformed input string {line}")

    return (prop_name.strip(), prop_value.strip(), comment.strip())

def main():
    """
    Entry point.  Reads two Java properties files, merges them according to
    the rules described in the module docstring, and prints the result.
    """
    args = parse_args()

    # The tuples prop_name, prop_value, comment of the 2 files
    parsed_file1  = []
    parsed_file2  = []

    # Name of the properties of the 2 files
    prop_name_file1 = []
    prop_name_file2 = []

    # Read the 2 files of java properties
    with open(args.PropFile1, "r") as p1_file:
        lines1 = p1_file.readlines()
        parsed_file1 = [ parse_line(line) for line in lines1 ]
        prop_name_file1 = [ prop_name for prop_name, prop_value, comment in parsed_file1 if prop_name ]
    with open(args.PropFile2, "r") as p2_file:
        lines2 = p2_file.readlines()
        parsed_file2 = [ parse_line(line) for line in lines2 ]
        prop_name_file2 = [ prop_name for prop_name, prop_value, comment in parsed_file2 if prop_name ]

    # generate the output

    # Print the content of the first file with replaced propes commemnted out
    for (name, value, comment) in parsed_file1:
        if (name=="" and len(value)>0) or (len(name)>0 and value==""):
            raise RuntimeError(f"Error getting prop name [{name}] and value [{value}] from {args.PropFile1}")

        if name:
            if name in prop_name_file2:
                print(f"#REPLACED {name}={value} {comment}")
            else:    
                print(f"{name}={value} {comment}")
        else:
            print(comment) # include name=value=comment==""

    if args.comment:
        print(f"\n##\n## {args.comment}\n##\n")
    for (name, value, comment) in parsed_file2:
        if (name=="" and len(value)>0) or (len(name)>0 and value==""):
                    raise RuntimeError(f"Error getting prop name [{name}] and value [{value}] from {args.PropFile2}")

        if name:
            print(f"{name}={value} {comment}")

if __name__ == '__main__':
    main()