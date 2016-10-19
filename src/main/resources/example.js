class WordLink extends React.Component {
    constructor() {
        super();
        console.log(this.props);
    }

    render() {
        return <li onClick={ () =>
            this.props.showWord(this.props.word)
        }>
            { this.props.word }
        </li>
    }
}

class MessageBox extends React.Component {
    constructor() {
        super();
        this.state = {selected: "president",
                      list: []
        };
    }

    fetchWord(word) {
        console.log("fetching " + word);
        this.setState( {selected: word} );
        return $.get({
            url: "/graphql",
            contentType: 'application/json',
            type: "POST",
            data: '{ "query":"{word(word:\\"' + word + '\\") { synonyms(numberOfWords:20) } }" }',
            processData: false,
            dataType: 'json',
            cache: false,
            success: ( response => {
                if (response.data) {
                    this.setState({list: response.data.word.synonyms});
                }
            })
        })
    }

    componentDidMount() {
        console.log("mounted");
        this.fetchWord(this.state.selected);
    }

    render() {
        console.log("rendering");
        return <div>
            <div>selected:
                <input
                    type="text"
                    value={this.state.selected}
                    onChange={ event => this.fetchWord(event.target.value) }
                />
            </div>
            <ul> {
                this.state.list.map( listValue => {
                    return <WordLink
                        word={listValue}
                        showWord={ word => { this.fetchWord(word) } }
                    />
                })
            } </ul>
        </div>
    }
}

ReactDOM.render(
<MessageBox />,
    document.getElementById('content')
);