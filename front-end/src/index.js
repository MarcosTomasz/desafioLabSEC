import React from 'react';
import ReactDOM from 'react-dom';
import axios from 'axios';
import DatePicker from "react-datepicker";

import "react-datepicker/dist/react-datepicker.css";

<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

class NameForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            nomeEntidade: "",
            data_hora_inicial: "",
            data_hora_final: "",
            gerar: false,
            consultar: true,
            lista: [],
            query: [],
            listaDelecao: [],
        };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.onSelect = this.onSelect.bind(this);
        this.addtoRemove = this.addtoRemove.bind(this);
        this.remove = this.remove.bind(this);
    }

    onSelect(opcao, date) {
        if (opcao === 0) {
            this.setState({
                data_hora_inicial: date,
            });
        } else {
            this.setState({
                data_hora_final: date,
            });
        }
    }

    remove(event) {
        let temp = ""
        for (let index = 0; index < this.state.listaDelecao.length; index++) {
            if (index != 0) {
                temp += ", "
            }
            temp += this.state.listaDelecao[index];
        }
        this.setState({
            listaDelecao: []
        })

        const data = {
            "listaDelecao": temp,
        }
        axios.post("http://127.0.0.1:8080/remover",  data, {                    
            headers: {                  
                "Access-Control-Allow-Origin": "*"               
            },
        })
        .then(res => {
            let listaCertificados = []
            let certificado = []
            for (let i = 0; i < res.data.length; i++) {
                certificado.push(res.data[i].index)
                certificado.push(res.data[i].titular)
                certificado.push(res.data[i].dataValidade)
                certificado.push(res.data[i].numeroSerie)
                certificado.push(res.data[i].chavePublica)
                listaCertificados.push(certificado)
                certificado= []
            }
            console.log(listaCertificados)

            this.setState({
                query: listaCertificados,
            })
        })
    }

    addtoRemove(event, object) {
        object.target.className = "px-2 btn btn-outline-danger btn-sm"
        object.target.textContent = "Aguardando Confirmação"
        this.setState({
            listaDelecao: this.state.listaDelecao.concat(object.target.id)
        })
    }

    handleChange(event) {
        this.setState({
            nomeEntidade: event.target.value,
        });
    }

    async handleSubmit(event) {
        if (this.state.consultar){
            const data = {
                "data_hora_inicial": event.target.data_hora_inicial.value,
                "data_hora_final": event.target.data_hora_final.value,
                "entidade": event.target.nomeEntidade.value
            }
            axios.post("http://127.0.0.1:8080/consulta",  data, {                    
                headers: {                  
                    "Access-Control-Allow-Origin": "*"               
                },
            })
            .then(res => {
                let listaCertificados = []
                let certificado = []
                for (let i = 0; i < res.data.length; i++) {
                    certificado.push(res.data[i].index)
                    certificado.push(res.data[i].titular)
                    certificado.push(res.data[i].dataInicioValidade)
                    certificado.push(res.data[i].dataFimValidade)
                    certificado.push(res.data[i].numeroSerie)
                    certificado.push(res.data[i].chavePublica)
                    listaCertificados.push(certificado)
                    certificado= []
                }

                this.setState({
                    query: listaCertificados,
                })
            })
        } else {
            if (event.target.nomeEntidade.value === "") {
                alert("Insira o nome da entidade")
            } else {
                if (event.target.data_hora_inicial.value.length === 0 || event.target.data_hora_final.value.length === 0){
                    alert("Selecione ambos os dias de vencimento")
                } else {
                    const data = {
                        "data_hora_inicial": event.target.data_hora_inicial.value,
                        "data_hora_final": event.target.data_hora_final.value,
                        "entidade": event.target.nomeEntidade.value
                    }
                    axios.post("http://127.0.0.1:8080/cert",  data, {                    
                        headers: {                  
                            "Access-Control-Allow-Origin": "*"               
                        },
                    })
                    .then(
                        alert("Certificado cadastrado com sucesso!"),
                        this.setState({
                            nomeEntidade: "",
                            data_hora_inicial: "",
                            data_hora_final: "",
                        })  
                    )
                }
            }
        }
        event.preventDefault();
    }
  
    render() {
        const listaHistorico = this.state.query.map(
            (cert, index) => {
                return (
                    <tr key={cert[0]}>
                        <div onClick={this.addtoRemove.bind(this,cert[0])} type="button" id={cert[0]} className="px-2 btn-sm active btn btn-outline-warning">Remover</div>
                        <td>{cert[1]}</td>
                        <td>{cert[2]}</td>
                        <td>{cert[3]}</td>
                        <td>{cert[4]}</td>
                        <td className="text-break">{cert[4]}</td>
                    </tr>
                );
            }
        );
        return (
            <section>
                <form className="py-auto text-center container w-80" onSubmit={this.handleSubmit}>
                    <label>
                        <div className="input-group text-center">
                            <input
                                type="name"
                                className="form-control"
                                name="nomeEntidade" 
                                onChange={this.handleChange}
                                value={this.state.nomeEntidade}
                                placeholder="Nome do Titular"
                            />
                            <span className="input-group-addon px-1 ">&nbsp;</span>
                            <DatePicker
                                placeholderText="Validade Inicial"
                                className="form-control"
                                name="data_hora_inicial"
                                selected={this.state.data_hora_inicial}
                                dateFormat="dd/MM/yyyy"
                                isClearable
                                closeOnScroll={true}
                                onChange={this.onSelect.bind(this, 0)}
                            />
                            <span className="input-group-addon px-1 ">&nbsp;</span>
                            <DatePicker
                                placeholderText="Validade Final"
                                className="form-control"
                                name="data_hora_final"
                                selected={this.state.data_hora_final}
                                dateFormat="dd/MM/yyyy"
                                isClearable
                                closeOnScroll={true}
                                onChange={this.onSelect.bind(this, 1)}
                            />
                        </div>
                        <div className="input-group text-center">
                            <span className="input-group-addon px-2 ">&nbsp;</span>
                        </div>
                        <button 
                            onClick={() => {
                                this.setState({
                                    gerar: true,
                                    consultar: false,
                                })
                            }} 
                            type="gerar" 
                            className="btn btn-primary float-right my-2">
                            Gerar
                        </button>
                        <span className="input-group-addon px-2 ">&nbsp;</span>
                        <button 
                            onClick={() => {
                                this.setState({
                                    gerar: false,
                                    consultar: true,
                                })
                            }} 
                            type="consulta" 
                            className="btn btn-secondary float-right my-3">
                            Consultar
                        </button>
                    </label>
                </form>
                <table className="table">
                    <thead>
                        <tr>
                            <th scope="col" className="p2 w-5"><div onClick={this.remove.bind(this)} type="remover" className="px-2 btn btn-outline-danger">Confirmar remoção</div></th>
                            <th scope="col" className="w-25">Nome</th>
                            <th scope="col">Válido desde</th>
                            <th scope="col">Válido até</th>
                            <th scope="col">Número Serial</th>
                            <th scope="col" className="w-75">Chave Pública</th>
                        </tr>
                    </thead>
                <tbody>
                    {listaHistorico}
                </tbody>
                </table>
            </section>
        );
    }
}

// ========================================

ReactDOM.render(
    <NameForm />,
document.getElementById('formulario')
);