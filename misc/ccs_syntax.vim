" Vim syntax file
" Language:    CCS
" Maintainers: Matt Hellige <matt@immute.net>
" Last change: 2011 December 19


syn clear

syn case match

syn match ccsComment "//.*"
syn region ccsComment start="/\*" end="\*/" contains=ccsComment

syn match ccsKeyword "@\(import\|context\|local\|override\)"

syn match ccsSpecialChar "(\|)\|{\|}\|:\|+\|\*\|,\|>\|="


if !exists("did_ccs_syntax_inits")

  let did_ccs_syntax_inits = 1

  hi link ccsComment     Comment
  hi link ccsKeyword     Keyword
  hi link ccsSpecialChar SpecialChar

endif

let b:current_syntax = "ccs"

" vim: ts=28
