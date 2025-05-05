# sat (separated cat)

`sat(1)` is cat-like tool but it can insert separator between files.

## SYNOPSIS

```sh
sat [-s] [-S separator] [file...]
```

## DESCRIPTION

The `sat` utility shall read files in sequence and shall write their contents to the standard output in the same sequence.

In addition, it can insert separator between files.

## OPTIONS

- `-s`, `--squeeze-blank`: suppress repeated empty output lines
- `-S separator`: break files using separator

## EXAMPLES

```sh
% echo foo > foo.txt
% echo bar > bar.txt
% sat foo.txt bar.txt
foo
bar
% sat -S "---\n" foo.txt bar.txt foo.txt
foo
---
bar
---
foo
%
```
