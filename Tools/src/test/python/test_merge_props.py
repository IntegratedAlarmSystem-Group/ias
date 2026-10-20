'''
Test iasMergeProps module
'''
import sys
import tempfile
import os
from io import StringIO

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', 'main', 'python'))
from iasMergeProps import parse_line


class TestParseLine():

    def test_empty_line(self):
        assert parse_line("") == ("", "", "")

    def test_whitespace_only_line(self):
        assert parse_line("   \t\n") == ("", "", "")

    def test_comment_only_line(self):
        name, value, comment = parse_line("# this is a comment")
        assert name == ""
        assert value == ""
        assert comment == "# this is a comment"

    def test_simple_property(self):
        name, value, comment = parse_line("key=value")
        assert name == "key"
        assert value == "value"
        assert comment == ""

    def test_property_with_comment(self):
        name, value, comment = parse_line("key=value # inline comment")
        assert name == "key"
        assert value == "value"
        assert comment == "# inline comment"

    def test_property_with_spaces(self):
        name, value, comment = parse_line(" key = value ")
        assert name == "key"
        assert value == "value"
        assert comment == ""

    def test_property_value_with_equals(self):
        name, value, comment = parse_line("key=val=ue")
        assert name == "key"
        assert value == "val=ue"
        assert comment == ""

    def test_malformed_line_raises(self):
        try:
            parse_line("noequals")
            assert False, "Expected ValueError"
        except ValueError:
            pass

    def test_property_with_comment_and_spaces(self):
        name, value, comment = parse_line("key = value # some comment")
        assert name == "key"
        assert value == "value"
        assert comment == "# some comment"

    def test_empty_value_raises(self):
        try:
            parse_line("key=")
            assert False, "Expected ValueError"
        except ValueError:
            pass

    def test_empty_name_raises(self):
        try:
            parse_line("=value")
            assert False, "Expected ValueError"
        except ValueError:
            pass


class TestMergeMain():

    def _create_temp_file(self, content):
        f = tempfile.NamedTemporaryFile(mode='w', suffix='.properties', delete=False)
        f.write(content)
        f.close()
        return f.name

    def _cleanup(self, *files):
        for f in files:
            if os.path.exists(f):
                os.unlink(f)

    def _run_merge(self, file1_content, file2_content, comment=None):
        f1 = self._create_temp_file(file1_content)
        f2 = self._create_temp_file(file2_content)
        try:
            old_argv = sys.argv
            old_stdout = sys.stdout
            sys.argv = ['iasMergeProps', f1, f2]
            if comment:
                sys.argv.extend(['-c', comment])

            captured = StringIO()
            sys.stdout = captured

            from iasMergeProps import main
            main()

            sys.stdout = old_stdout
            sys.argv = old_argv

            return captured.getvalue()
        finally:
            self._cleanup(f1, f2)

    def test_properties_only_in_file1_preserved(self):
        file1 = "key1=value1\n"
        file2 = "key2=value2\n"
        output = self._run_merge(file1, file2)
        lines = output.strip().split('\n')
        assert "key1=value1" in lines[0]
        assert "key2=value2" in lines[1]

    def test_properties_only_in_file2_appended(self):
        file1 = "key1=value1\n"
        file2 = "key2=value2\n"
        output = self._run_merge(file1, file2)
        lines = output.strip().split('\n')
        assert "key2=value2" in lines

    def test_overridden_property_commented_and_replaced(self):
        file1 = "key1=value1\n"
        file2 = "key1=overridden\n"
        output = self._run_merge(file1, file2)
        lines = output.strip().split('\n')
        assert "#REPLACED key1=value1" in lines[0]
        assert "key1=overridden" in lines[1]

    def test_preserves_comments_from_file1(self):
        file1 = "key1=value1 # comment1\n"
        file2 = "key2=value2\n"
        output = self._run_merge(file1, file2)
        assert "# comment1" in output

    def test_preserves_comment_only_lines(self):
        file1 = "# header comment\nkey1=value1\n"
        file2 = "key2=value2\n"
        output = self._run_merge(file1, file2)
        lines = output.strip().split('\n')
        assert "# header comment" in lines[0]

    def test_preserves_empty_lines_from_file1(self):
        file1 = "key1=value1\n\nkey3=value3\n"
        file2 = "key2=value2\n"
        output = self._run_merge(file1, file2)
        lines = output.split('\n')
        assert "" in lines

    def test_comment_flag_adds_header(self):
        file1 = "key1=value1\n"
        file2 = "key2=value2\n"
        output = self._run_merge(file1, file2, comment="Properties from file2")
        assert "Properties from file2" in output
        # Verify the comment appears before file2 properties
        comment_idx = output.index("Properties from file2")
        key2_idx = output.index("key2=value2")
        assert comment_idx < key2_idx

    def test_multiple_properties_mixed(self):
        file1 = "a=1\nb=2\nc=3\n"
        file2 = "b=overridden\nd=4\n"
        output = self._run_merge(file1, file2)
        lines = output.strip().split('\n')
        assert "a=1" in lines[0]
        assert "#REPLACED b=2" in lines[1]
        assert "c=3" in lines[2]
        assert "b=overridden" in lines[3]
        assert "d=4" in lines[4]
