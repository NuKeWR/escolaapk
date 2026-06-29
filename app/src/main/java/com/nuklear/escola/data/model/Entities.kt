package com.nuklear.escola.data.model

import androidx.room.*

@Entity(tableName = "alunos")
data class Aluno(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val sexo: String = "",
    @ColumnInfo(name = "data_nascimento") val dataNascimento: String = "",
    @ColumnInfo(name = "faixa_atual") val faixaAtual: String = "",
    val tipo: String = "",                    // Aluno, Bolsista, Professor
    @ColumnInfo(name = "valor_mensalidade") val valorMensalidade: Double = 0.0,
    @ColumnInfo(name = "dia_vencimento") val diaVencimento: Int = 5,
    val telefone: String = "",
    val responsavel: String = "",
    @ColumnInfo(name = "data_matricula") val dataMatricula: String = "",
    val turma: String = "",                   // Kids, Adulto
    val horario: String = "",
    val modalidade: String = "",
    val observacoes: String = "",
    val ativo: Int = 1,
    @ColumnInfo(name = "foto_path") val fotoPath: String = ""
)

@Entity(
    tableName = "pagamentos",
    foreignKeys = [ForeignKey(
        entity = Aluno::class,
        parentColumns = ["id"],
        childColumns = ["aluno_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("aluno_id")]
)
data class Pagamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "aluno_id") val alunoId: Long,
    @ColumnInfo(name = "data_pagamento") val dataPagamento: String,
    val competencia: String,
    @ColumnInfo(name = "valor_pago") val valorPago: Double,
    @ColumnInfo(name = "forma_pagamento") val formaPagamento: String = "",
    val observacao: String = ""
)

@Entity(
    tableName = "graduacoes",
    foreignKeys = [ForeignKey(
        entity = Aluno::class,
        parentColumns = ["id"],
        childColumns = ["aluno_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("aluno_id")]
)
data class Graduacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "aluno_id") val alunoId: Long,
    @ColumnInfo(name = "data_graduacao") val dataGraduacao: String,
    val faixa: String,
    val observacao: String = ""
)

@Entity(
    tableName = "face_embeddings",
    foreignKeys = [ForeignKey(
        entity = Aluno::class,
        parentColumns = ["id"],
        childColumns = ["aluno_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("aluno_id")]
)
data class FaceEmbedding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "aluno_id") val alunoId: Long,
    val embedding: String,           // JSON array de 128 floats
    @ColumnInfo(name = "created_at") val createdAt: String = ""
)

@Entity(tableName = "presencas")
data class Presenca(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: String,
    val aluno: String
)
